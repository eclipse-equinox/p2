/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata.generator;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.frameworkadmin.equinox.EquinoxConstants;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.metadata.generator.*;
import org.eclipse.equinox.internal.p2.metadata.generator.Messages;
import org.eclipse.equinox.internal.p2.metadata.generator.features.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Version;

public class Generator {
	/**
	 * Captures the output of an execution of the generator.
	 */
	protected static final String ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR = "org.eclipse.equinox.simpleconfigurator"; //$NON-NLS-1$
	protected static final String ORG_ECLIPSE_UPDATE_CONFIGURATOR = "org.eclipse.update.configurator"; //$NON-NLS-1$
	protected static final String ORG_ECLIPSE_EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher"; //$NON-NLS-1$

	static final String DEFAULT_BUNDLE_LOCALIZATION = "plugin"; //$NON-NLS-1$	
	public static final String CONFIG_SEGMENT_SEPARATOR = "."; //$NON-NLS-1$

	private final IGeneratorInfo info;

	private IPublisherResult incrementalResult = null;
	private final IProductDescriptor product = null;
	private boolean generateRootIU = true;

	/**
	 * Short term fix to ensure IUs that have no corresponding category are not lost.
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=211521.
	 */
	protected final Set rootCategory = new HashSet();

	private StateObjectFactory stateObjectFactory;

	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified.
	 */
	public static String[] getArrayFromString(String list, String separator) {
		if (list == null || list.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		List result = new ArrayList();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				result.add(token);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	/**
	 * Returns a string array of { ws, os, arch } as parsed from the given string
	 * @param configSpec the string to parse
	 * @return the ws, os, arch form of the given string
	 */
	public static String[] parseConfigSpec(String configSpec) {
		String[] result = Generator.getArrayFromString(configSpec, CONFIG_SEGMENT_SEPARATOR);
		return result;
	}

	/**
	 * Returns the LDAP filter form that matches the given config spec.  Returns
	 * an empty String if the spec does not identify an ws, os or arch.
	 * @param configSpec a config spec to filter
	 * @return the LDAP filter for the given spec.  
	 */
	public static String createFilterSpec(String configSpec) {
		String[] config = Generator.parseConfigSpec(configSpec);
		if (config[0] != null || config[1] != null || config[2] != null) {
			String filterWs = config[0] != null ? "(osgi.ws=" + config[0] + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String filterOs = config[1] != null ? "(osgi.os=" + config[1] + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String filterArch = config[2] != null ? "(osgi.arch=" + config[2] + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return "(& " + filterWs + filterOs + filterArch + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Returns the normalized string form of the given config spec.  This is useful for putting
	 * in IU ids etc. Note that the result is not intended to be machine readable (i.e., parseConfigSpec
	 * may not work on the result).
	 * @param configSpec the config spec to format
	 * @return the readable format of the given config spec
	 */
	public static String createIdString(String configSpec) {
		String[] config = Generator.parseConfigSpec(configSpec);
		return config[0] + '.' + config[1] + '.' + config[2];
	}

	/**
	 * Returns the canonical form of config spec with the given ws, os and arch.
	 * Note that the result is intended to be machine readable (i.e., parseConfigSpec
	 * will parse the the result).
	 * @param ws the window system
	 * @param os the operating system
	 * @param arch the machine architecture
	 * @return the machine readable format of the given config spec
	 */
	public static String createConfigSpec(String ws, String os, String arch) {
		return ws + '.' + os + '.' + arch;
	}

	public Generator(IGeneratorInfo infoProvider) {
		this.info = infoProvider;
		// TODO need to figure a better way of configuring the generator...
		PlatformAdmin platformAdmin = (PlatformAdmin) ServiceHelper.getService(Activator.getContext(), PlatformAdmin.class.getName());
		if (platformAdmin != null) {
			stateObjectFactory = platformAdmin.getFactory();
		}
	}

	public void setIncrementalResult(IPublisherResult result) {
		this.incrementalResult = result;
	}

	protected IInstallableUnit createProductIU(IPublisherResult result) {
		generateProductConfigCUs(result);

		IPublisherResult productContents = new PublisherResult();

		ProductQuery query = new ProductQuery(product, info.getFlavor(), result.getFragmentMap(), info.getVersionAdvice());
		Collector collector = info.getMetadataRepository().query(query, query.getCollector(), null);
		for (Iterator iterator = collector.iterator(); iterator.hasNext();) {
			productContents.addIU((IInstallableUnit) iterator.next(), IPublisherResult.ROOT);
		}

		String version = product.getVersion();
		if (version.equals("0.0.0") && info.getRootVersion() != null) //$NON-NLS-1$
			version = info.getRootVersion();
		ArrayList requires = new ArrayList(1);
		requires.add(MetadataFactory.createRequiredCapability(info.getFlavor() + product.getId(), product.getId() + ".launcher", VersionRange.emptyRange, null, false, true)); //$NON-NLS-1$
		requires.add(MetadataFactory.createRequiredCapability(info.getFlavor() + product.getId(), product.getId() + ".ini", VersionRange.emptyRange, null, false, false)); //$NON-NLS-1$
		requires.add(MetadataFactory.createRequiredCapability(info.getFlavor() + product.getId(), product.getId() + ".config", VersionRange.emptyRange, null, false, false)); //$NON-NLS-1$

		//default CUs		
		requires.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, MetadataGeneratorHelper.createDefaultConfigUnitId(MetadataGeneratorHelper.OSGI_BUNDLE_CLASSIFIER, info.getFlavor()), VersionRange.emptyRange, null, false, false));
		requires.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, MetadataGeneratorHelper.createDefaultConfigUnitId("source", info.getFlavor()), VersionRange.emptyRange, null, true, false)); //$NON-NLS-1$
		if (product.useFeatures())
			requires.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, MetadataGeneratorHelper.createDefaultConfigUnitId(MetadataGeneratorHelper.ECLIPSE_FEATURE_CLASSIFIER, info.getFlavor()), VersionRange.emptyRange, MetadataGeneratorHelper.INSTALL_FEATURES_FILTER, true, false));

		InstallableUnitDescription root = createTopLevelIUDescription(productContents.getIUs(null, IPublisherResult.ROOT), product.getId(), version, product.getProductName(), requires, false);
		return MetadataFactory.createInstallableUnit(root);
	}

	protected IInstallableUnit createTopLevelIU(IPublisherResult result, String configurationIdentification, String configurationVersion) {
		// TODO, bit of a hack but for now set the name of the IU to the ID.
		InstallableUnitDescription root = createTopLevelIUDescription(result.getIUs(null, IPublisherResult.ROOT), configurationIdentification, configurationVersion, configurationIdentification, null, true);
		return MetadataFactory.createInstallableUnit(root);
	}

	protected InstallableUnitDescription createTopLevelIUDescription(Collection children, String configurationIdentification, String configurationVersion, String configurationName, List requires, boolean configureLauncherData) {
		InstallableUnitDescription root = new MetadataFactory.InstallableUnitDescription();
		root.setSingleton(true);
		root.setId(configurationIdentification);
		root.setVersion(new Version(configurationVersion));
		root.setProperty(IInstallableUnit.PROP_NAME, configurationName);

		ArrayList reqsConfigurationUnits = new ArrayList(children.size());
		for (Iterator iterator = children.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
			//			boolean isOptional = checkOptionalRootDependency(iu);
			reqsConfigurationUnits.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, iu.getFilter(), false, false));
		}
		if (requires != null)
			reqsConfigurationUnits.addAll(requires);
		root.setRequiredCapabilities((RequiredCapability[]) reqsConfigurationUnits.toArray(new RequiredCapability[reqsConfigurationUnits.size()]));
		root.setArtifacts(new IArtifactKey[0]);

		root.setProperty("lineUp", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		root.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(configurationIdentification, VersionRange.emptyRange, IUpdateDescriptor.NORMAL, null));
		root.setProperty(IInstallableUnit.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		root.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(configurationIdentification, new Version(configurationVersion))});
		root.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_OSGI);
		Map touchpointData = new HashMap();

		String configurationData = ""; //$NON-NLS-1$
		String unconfigurationData = ""; //$NON-NLS-1$

		ConfigData configData = info.getConfigData();
		if (configData != null) {
			String[] dataStrings = getConfigurationStrings(configData);
			configurationData += dataStrings[0];
			unconfigurationData += dataStrings[1];
		}

		if (configureLauncherData) {
			LauncherData launcherData = info.getLauncherData();
			if (launcherData != null) {
				String[] dataStrings = getLauncherConfigStrings(launcherData.getJvmArgs(), launcherData.getProgramArgs());
				configurationData += dataStrings[0];
				unconfigurationData += dataStrings[1];
			}
		}

		touchpointData.put("configure", configurationData); //$NON-NLS-1$
		touchpointData.put("unconfigure", unconfigurationData); //$NON-NLS-1$
		root.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		return root;
	}

	protected String[] getConfigurationStrings(ConfigData configData) {
		String configurationData = ""; //$NON-NLS-1$
		String unconfigurationData = ""; //$NON-NLS-1$
		for (Iterator iterator = configData.getFwDependentProps().entrySet().iterator(); iterator.hasNext();) {
			Entry aProperty = (Entry) iterator.next();
			String key = ((String) aProperty.getKey());
			if (key.equals("osgi.frameworkClassPath") || key.equals("osgi.framework") || key.equals("osgi.bundles") || key.equals("eof")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				continue;
			configurationData += "setProgramProperty(propName:" + key + ", propValue:" + ((String) aProperty.getValue()) + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			unconfigurationData += "setProgramProperty(propName:" + key + ", propValue:);"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		for (Iterator iterator = configData.getFwIndependentProps().entrySet().iterator(); iterator.hasNext();) {
			Entry aProperty = (Entry) iterator.next();
			String key = ((String) aProperty.getKey());
			if (key.equals("osgi.frameworkClassPath") || key.equals("osgi.framework") || key.equals("osgi.bundles") || key.equals("eof")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				continue;
			configurationData += "setProgramProperty(propName:" + key + ", propValue:" + ((String) aProperty.getValue()) + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			unconfigurationData += "setProgramProperty(propName:" + key + ", propValue:);"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		return new String[] {configurationData, unconfigurationData};
	}

	protected String[] getLauncherConfigStrings(final String[] jvmArgs, final String[] programArgs) {
		String configurationData = ""; //$NON-NLS-1$
		String unconfigurationData = ""; //$NON-NLS-1$

		for (int i = 0; i < jvmArgs.length; i++) {
			configurationData += "addJvmArg(jvmArg:" + jvmArgs[i] + ");"; //$NON-NLS-1$ //$NON-NLS-2$
			unconfigurationData += "removeJvmArg(jvmArg:" + jvmArgs[i] + ");"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		for (int i = 0; i < programArgs.length; i++) {
			String programArg = programArgs[i];
			if (programArg.equals("--launcher.library") || programArg.equals("-startup") || programArg.equals("-configuration")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				i++;
			configurationData += "addProgramArg(programArg:" + programArg + ");"; //$NON-NLS-1$ //$NON-NLS-2$
			unconfigurationData += "removeProgramArg(programArg:" + programArg + ");"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return new String[] {configurationData, unconfigurationData};
	}

	public IStatus generate() {
		IPublisherResult result = incrementalResult != null ? incrementalResult : new PublisherResult();

		Feature[] features = getFeatures(info.getFeaturesLocation());
		generateFeatureIUs(features, result, info.getArtifactRepository());

		BundleDescription[] bundles = getBundleDescriptions(info.getBundleLocations());
		generateBundleIUs(bundles, result, info.getArtifactRepository());

		generateNativeIUs(info.getExecutableLocation(), result, info.getArtifactRepository());

		generateConfigIUs(result);

		if (info.addDefaultIUs())
			generateDefaultConfigIU(result);

		if (generateRootIU)
			generateRootIU(result, info.getRootId(), info.getRootVersion());

		//		persistence.setMappingRules(info.getMappingRules() == null ? defaultMappingRules : info.getMappingRules());
		//		if (info.publishArtifacts() || info.publishArtifactRepository()) {
		//			persistence.saveArtifactRepository();
		//		}
		IMetadataRepository metadataRepository = info.getMetadataRepository();
		if (metadataRepository != null) {
			Collection allGeneratedUnits = result.getIUs(null, null);
			metadataRepository.addInstallableUnits((IInstallableUnit[]) allGeneratedUnits.toArray(new IInstallableUnit[allGeneratedUnits.size()]));
		}

		return Status.OK_STATUS;
	}

	protected void generateBundleIUs(BundleDescription[] bundles, IPublisherResult result, IArtifactRepository destination) {
		// Computing the path for localized property files in a NL fragment bundle
		// requires the BUNDLE_LOCALIZATION property from the manifest of the host bundle,
		// so a first pass is done over all the bundles to cache this value as well as the tags
		// from the manifest for the localizable properties.
		final int CACHE_PHASE = 0;
		final int GENERATE_PHASE = 1;
		final int BUNDLE_LOCALIZATION_INDEX = MetadataGeneratorHelper.BUNDLE_LOCALIZATION_INDEX;
		Map bundleLocalizationMap = new HashMap(bundles.length);
		Set localizationIUs = new HashSet(32);
		for (int phase = CACHE_PHASE; phase <= GENERATE_PHASE; phase++) {
			for (int i = 0; i < bundles.length; i++) {
				BundleDescription bd = bundles[i];
				// A bundle may be null if the associated plug-in does not have a manifest file -
				// for example, org.eclipse.jdt.launching.j9
				if (bd != null && bd.getSymbolicName() != null && bd.getVersion() != null) {
					Map bundleManifest = (Map) bd.getUserObject();

					if (phase == CACHE_PHASE) {
						if (bundleManifest != null) {
							String[] cachedValues = MetadataGeneratorHelper.getManifestCachedValues(bundleManifest);
							bundleLocalizationMap.put(makeSimpleKey(bd), cachedValues);
						}
					} else {
						String format = (String) (bundleManifest).get(BundleDescriptionFactory.BUNDLE_FILE_KEY);
						boolean isDir = (format != null && format.equals(BundleDescriptionFactory.DIR) ? true : false);

						IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(bd.getSymbolicName(), bd.getVersion().toString());
						IArtifactDescriptor ad = MetadataGeneratorHelper.createArtifactDescriptor(key, new File(bd.getLocation()), true, false);
						if (isDir)
							publishArtifact(ad, new File(bd.getLocation()).listFiles(), destination, false, true);
						else
							publishArtifact(ad, new File[] {new File(bd.getLocation())}, destination, true, true);
						if (info.reuseExistingPack200Files() && !info.publishArtifacts()) {
							File packFile = new Path(bd.getLocation()).addFileExtension("pack.gz").toFile(); //$NON-NLS-1$
							if (packFile.exists()) {
								IArtifactDescriptor ad200 = MetadataGeneratorHelper.createPack200ArtifactDescriptor(key, packFile, ad.getProperty(IArtifactDescriptor.ARTIFACT_SIZE));
								publishArtifact(ad200, new File[] {packFile}, destination, true, true);
							}
						}

						IInstallableUnit bundleIU = MetadataGeneratorHelper.createBundleIU(bd, bundleManifest, isDir, key, localizationIUs);

						if (isFragment(bd)) {
							// TODO: Can NL fragments be multi-host?  What special handling
							//		 is required for multi-host fragments in general?
							String hostId = bd.getHost().getName();
							String hostKey = makeSimpleKey(hostId);
							String[] cachedValues = (String[]) bundleLocalizationMap.get(hostKey);

							if (cachedValues != null) {
								MetadataGeneratorHelper.createHostLocalizationFragments(bd, hostId, cachedValues, localizationIUs);
							}
						}

						result.addIU(bundleIU, IPublisherResult.ROOT);
						result.addIUs(localizationIUs, IPublisherResult.NON_ROOT);
						localizationIUs.clear();
					}
				}
			}
		}
	}

	private static boolean isFragment(BundleDescription bd) {
		return (bd.getHost() != null ? true : false);
	}

	private static String makeSimpleKey(BundleDescription bd) {
		// TODO: can't use the bundle version in the key for the BundleLocalization
		//		 property map since the host specification for a fragment has a
		//		 version range, not a version. Hence, this mechanism for finding
		// 		 manifest localization property files may break under changes
		//		 to the BundleLocalization property of a bundle.
		return makeSimpleKey(bd.getSymbolicName() /*, bd.getVersion() */);
	}

	private static String makeSimpleKey(String id /*, Version version */) {
		return id; // + '_' + version.toString();
	}

	/**
	 * Generates IUs corresponding to update site categories.
	 * @param categoriesToFeatures Map of SiteCategory ->Set (Feature IUs in that category).
	 * @param result The generator result being built
	 */
	protected void generateCategoryIUs(Map categoriesToFeatures, IPublisherResult result) {
		for (Iterator it = categoriesToFeatures.keySet().iterator(); it.hasNext();) {
			SiteCategory category = (SiteCategory) it.next();
			result.addIU(MetadataGeneratorHelper.createCategoryIU(category, (Set) categoriesToFeatures.get(category), null), IPublisherResult.NON_ROOT);
		}
	}

	protected void storeConfigData(IPublisherResult result, String configuration) {
		if (result.getConfigData().containsKey(configuration))
			return; //been here, done this

		File fwConfigFile = new File(info.getLauncherData().getFwConfigLocation(), EquinoxConstants.CONFIG_INI);
		if (fwConfigFile.exists()) {
			if (info instanceof EclipseInstallGeneratorInfoProvider) {
				((EclipseInstallGeneratorInfoProvider) info).loadConfigData(fwConfigFile);
				ConfigData data = info.getConfigData();
				result.getConfigData().put(configuration, data);
			}
		}
	}

	protected GeneratorBundleInfo createGeneratorBundleInfo(BundleInfo bundleInfo, IPublisherResult result) {
		if (bundleInfo.getLocation() != null)
			return new GeneratorBundleInfo(bundleInfo);

		String name = bundleInfo.getSymbolicName();

		//easy case: do we have a matching IU?
		IInstallableUnit iu = result.getIU(name, null);
		if (iu != null) {
			bundleInfo.setVersion(iu.getVersion().toString());
			return new GeneratorBundleInfo(bundleInfo);
		}

		//harder: try id_version
		int i = name.indexOf('_');
		while (i > -1) {
			Version version = null;
			try {
				version = new Version(name.substring(i));
				bundleInfo.setSymbolicName(name.substring(0, i));
				bundleInfo.setVersion(version.toString());
				return new GeneratorBundleInfo(bundleInfo);
			} catch (IllegalArgumentException e) {
				// the '_' found was probably part of the symbolic id
				i = name.indexOf('_', i);
			}
		}

		return null;
	}

	protected void generateBundleConfigIUs(BundleInfo[] infos, IPublisherResult result, String launcherConfig) {
		if (infos == null)
			return;

		String cuIdPrefix = ""; //$NON-NLS-1$
		String filter = null;
		if (launcherConfig != null) {
			cuIdPrefix = createIdString(launcherConfig);
			filter = createFilterSpec(launcherConfig);
		}

		for (int i = 0; i < infos.length; i++) {
			GeneratorBundleInfo bundle = createGeneratorBundleInfo(infos[i], result);
			if (bundle == null)
				continue;

			if (bundle.getSymbolicName().equals(ORG_ECLIPSE_UPDATE_CONFIGURATOR)) {
				bundle.setStartLevel(BundleInfo.NO_LEVEL);
				bundle.setMarkedAsStarted(false);
				bundle.setSpecialConfigCommands("setProgramProperty(propName:org.eclipse.update.reconcile, propValue:false);"); //$NON-NLS-1$
				bundle.setSpecialUnconfigCommands("setProgramProperty(propName:org.eclipse.update.reconcile, propValue:);"); //$NON-NLS-1$
			} else if (bundle.getStartLevel() == BundleInfo.NO_LEVEL && !bundle.isMarkedAsStarted()) {
				// this bundle does not require any particular configuration, the plug-in default IU will handle installing it
				continue;
			}

			IInstallableUnit cu = MetadataGeneratorHelper.createBundleConfigurationUnit(bundle.getSymbolicName(), new Version(bundle.getVersion()), false, bundle, info.getFlavor() + cuIdPrefix, filter);
			if (cu != null) {
				// Product Query will run against the repo, make sure these CUs are in before then
				IMetadataRepository metadataRepository = info.getMetadataRepository();
				if (metadataRepository != null) {
					metadataRepository.addInstallableUnits(new IInstallableUnit[] {cu});
				}
				result.addIU(cu, IPublisherResult.ROOT);
				String key = (product != null && product.useFeatures()) ? IPublisherResult.CONFIGURATION_CUS : bundle.getSymbolicName();
				result.addFragment(key, cu);
			}
		}

	}

	protected void generateConfigIUs(IPublisherResult result) {
		ConfigData data = info.getConfigData();
		if ((data == null || data.getBundles().length == 0) && info.getLauncherConfig() != null) {
			//We have the config.ini but not necessarily all the needed bundle IUs, remember for later
			storeConfigData(result, info.getLauncherConfig());
		} else if (data != null) {
			// generation against an eclipse install (config.ini + bundles)
			generateBundleConfigIUs(data.getBundles(), result, info.getLauncherConfig());
		} else if (result.getConfigData().size() > 0 && generateRootIU) {
			// generation from remembered config.ini's
			// we have N platforms, generate a CU for each
			// TODO try and find common properties across platforms
			for (Iterator iterator = result.getConfigData().keySet().iterator(); iterator.hasNext();) {
				String launcherConfig = (String) iterator.next();
				data = (ConfigData) result.getConfigData().get(launcherConfig);
				generateBundleConfigIUs(data.getBundles(), result, launcherConfig);
			}
		}

		List bundleInfoList = new ArrayList();
		if (info.addDefaultIUs())
			bundleInfoList.addAll(info.getDefaultIUs(result.getIUs(null, IPublisherResult.ROOT)));

		bundleInfoList.addAll(info.getOtherIUs());

		for (Iterator iterator = bundleInfoList.iterator(); iterator.hasNext();) {
			GeneratorBundleInfo bundle = (GeneratorBundleInfo) iterator.next();
			IInstallableUnit configuredIU = result.getIU(bundle.getSymbolicName(), null);
			if (configuredIU == null)
				continue;
			bundle.setVersion(configuredIU.getVersion().toString());
			String filter = configuredIU == null ? null : configuredIU.getFilter();
			IInstallableUnit cu = MetadataGeneratorHelper.createBundleConfigurationUnit(bundle.getSymbolicName(), new Version(bundle.getVersion()), false, bundle, info.getFlavor(), filter);
			//the configuration unit should share the same platform filter as the IU being configured.
			if (cu != null)
				result.addIU(cu, IPublisherResult.ROOT);
			if (bundle.getSymbolicName().startsWith(ORG_ECLIPSE_EQUINOX_LAUNCHER + '.'))
				result.addFragment(ORG_ECLIPSE_EQUINOX_LAUNCHER, cu);
		}
	}

	/**
	 * Short term fix to ensure IUs that have no corresponding category are not lost.
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=211521.
	 */
	protected IInstallableUnit generateDefaultCategory(IInstallableUnit rootIU, Collection defaultCategory) {
		defaultCategory.add(rootIU);

		InstallableUnitDescription cat = new MetadataFactory.InstallableUnitDescription();
		cat.setSingleton(true);
		String categoryId = rootIU.getId() + ".categoryIU"; //$NON-NLS-1$
		cat.setId(categoryId);
		cat.setVersion(Version.emptyVersion);
		cat.setProperty(IInstallableUnit.PROP_NAME, rootIU.getProperty(IInstallableUnit.PROP_NAME));
		cat.setProperty(IInstallableUnit.PROP_DESCRIPTION, rootIU.getProperty(IInstallableUnit.PROP_DESCRIPTION));

		ArrayList required = new ArrayList(defaultCategory.size());
		for (Iterator iterator = defaultCategory.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			required.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), VersionRange.emptyRange, iu.getFilter(), false, false));
		}
		cat.setRequiredCapabilities((RequiredCapability[]) required.toArray(new RequiredCapability[required.size()]));
		cat.setCapabilities(new ProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, categoryId, Version.emptyVersion)});
		cat.setArtifacts(new IArtifactKey[0]);
		cat.setProperty(IInstallableUnit.PROP_TYPE_CATEGORY, "true"); //$NON-NLS-1$
		return MetadataFactory.createInstallableUnit(cat);
	}

	protected void generateDefaultConfigIU(IPublisherResult result) {
		//		TODO this is a bit of a hack.  We need to have the default IU fragment generated with code that configures
		//		and unconfigures.  The Generator should be decoupled from any particular provider but it is not clear
		//		that we should add the create* methods to IGeneratorInfo...
		//		MockBundleDescription bd1 = new MockBundleDescription("defaultConfigure");
		//		MockBundleDescription bd2 = new MockBundleDescription("defaultUnconfigure");
		EclipseInstallGeneratorInfoProvider provider = (EclipseInstallGeneratorInfoProvider) info;
		result.addIU(MetadataGeneratorHelper.createDefaultBundleConfigurationUnit(provider.createDefaultConfigurationBundleInfo(), provider.createDefaultUnconfigurationBundleInfo(), info.getFlavor()), IPublisherResult.ROOT);
		result.addIU(MetadataGeneratorHelper.createDefaultFeatureConfigurationUnit(info.getFlavor()), IPublisherResult.ROOT);
		result.addIU(MetadataGeneratorHelper.createDefaultConfigurationUnitForSourceBundles(info.getFlavor()), IPublisherResult.ROOT);
	}

	/**
	 * This method generates IUs for the launchers found in the org.eclipse.executable feature, if present.
	 * @return <code>true</code> if the executable feature was processed successfully,
	 * and <code>false</code> otherwise.
	 */
	private boolean generateExecutableFeatureIUs(IPublisherResult result, IArtifactRepository destination) {
		File parentDir = info.getFeaturesLocation();
		if (parentDir == null || !parentDir.exists())
			return false;
		File[] featureDirs = parentDir.listFiles();
		if (featureDirs == null)
			return false;
		File executableFeatureDir = null;
		final String featurePrefix = "org.eclipse.equinox.executable_"; //$NON-NLS-1$
		for (int i = 0; i < featureDirs.length; i++) {
			if (featureDirs[i].getName().startsWith(featurePrefix)) {
				executableFeatureDir = featureDirs[i];
				break;
			}
		}
		if (executableFeatureDir == null)
			return false;
		File binDir = new File(executableFeatureDir, "bin"); //$NON-NLS-1$
		if (!binDir.exists())
			return false;
		//the bin directory is dividing into a directory tree of the form /bin/ws/os/arch
		File[] wsDirs = binDir.listFiles();
		if (wsDirs == null)
			return false;
		String versionString = executableFeatureDir.getName().substring(featurePrefix.length());
		for (int wsIndex = 0; wsIndex < wsDirs.length; wsIndex++) {
			String ws = wsDirs[wsIndex].getName();
			File[] osDirs = wsDirs[wsIndex].listFiles();
			if (osDirs == null)
				continue;
			for (int osIndex = 0; osIndex < osDirs.length; osIndex++) {
				String os = osDirs[osIndex].getName();
				File[] archDirs = osDirs[osIndex].listFiles();
				if (archDirs == null)
					continue;
				for (int archIndex = 0; archIndex < archDirs.length; archIndex++) {
					String arch = archDirs[archIndex].getName();
					generateExecutableIUs(createConfigSpec(ws, os, arch), versionString, archDirs[archIndex], result, destination);
				}
			}
		}
		return true;
	}

	/**
	 * Generates IUs and CUs for the files that make up the launcher for a given
	 * ws/os/arch combination.
	 */
	protected void generateExecutableIUs(String configSpec, String version, File root, IPublisherResult result, IArtifactRepository destination) {
		//Create the IU
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(true);
		String productNamespace = (product != null) ? product.getId() : "org.eclipse"; //$NON-NLS-1$
		String launcherIdPrefix = productNamespace + ".launcher"; //$NON-NLS-1$
		String launcherId = launcherIdPrefix + '.' + createIdString(configSpec);
		iu.setId(launcherId);
		Version launcherVersion = new Version(version);
		iu.setVersion(launcherVersion);
		String filter = createFilterSpec(configSpec);
		iu.setFilter(filter);

		IArtifactKey key = MetadataGeneratorHelper.createLauncherArtifactKey(launcherId, launcherVersion);
		iu.setArtifacts(new IArtifactKey[] {key});
		iu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_NATIVE);
		ProvidedCapability launcherCapability = MetadataFactory.createProvidedCapability(info.getFlavor() + productNamespace, launcherIdPrefix, new Version("1.0.0")); //$NON-NLS-1$
		iu.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(launcherId, launcherVersion), launcherCapability});

		// setup a requirement between the executable and the launcher fragment that has the shared library
		String[] config = parseConfigSpec(configSpec);
		String ws = config[0];
		String os = config[1];
		String arch = config[2];
		String launcherFragment = ORG_ECLIPSE_EQUINOX_LAUNCHER + '.' + ws + '.' + os;
		if (!Constants.OS_MACOSX.equals(os))
			launcherFragment += '.' + arch;
		iu.setRequiredCapabilities(new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, launcherFragment, VersionRange.emptyRange, filter, false, false)});
		result.addIU(MetadataFactory.createInstallableUnit(iu), IPublisherResult.ROOT);

		//Create the CU
		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = info.getFlavor() + launcherId;
		cu.setId(configUnitId);
		cu.setVersion(launcherVersion);
		cu.setFilter(filter);
		cu.setHost(new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, launcherId, new VersionRange(launcherVersion, true, launcherVersion, true), null, false, false)});
		cu.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		//TODO bug 218890, would like the fragment to provide the launcher capability as well, but can't right now.
		cu.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(configUnitId, launcherVersion)});

		mungeLauncherFileNames(root);

		cu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_NATIVE);
		Map touchpointData = new HashMap();
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		if (Constants.OS_MACOSX.equals(os)) {
			File[] appFolders = root.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.substring(name.length() - 4, name.length()).equalsIgnoreCase(".app"); //$NON-NLS-1$
				}
			});
			for (int i = 0; appFolders != null && i < appFolders.length; i++) {
				File macOSFolder = new File(appFolders[i], "Contents/MacOS"); //$NON-NLS-1$
				if (macOSFolder.exists()) {
					File[] launcherFiles = macOSFolder.listFiles();
					for (int j = 0; j < launcherFiles.length; j++) {
						configurationData += " chmod(targetDir:${installFolder}/" + appFolders[i].getName() + "/Contents/MacOS/, targetFile:" + launcherFiles[j].getName() + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						if (new Path(launcherFiles[j].getName()).getFileExtension() == null)
							MetadataGeneratorHelper.generateLauncherSetter(launcherFiles[j].getName(), launcherId, launcherVersion, configSpec, result);
					}
				}
			}
		}
		if (!Constants.OS_WIN32.equals(os) && !Constants.OS_MACOSX.equals(os)) {
			File[] launcherFiles = root.listFiles();
			for (int i = 0; i < launcherFiles.length; i++) {
				configurationData += " chmod(targetDir:${installFolder}, targetFile:" + launcherFiles[i].getName() + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$
				if (launcherFiles[i].isFile() && new Path(launcherFiles[i].getName()).getFileExtension() == null)
					MetadataGeneratorHelper.generateLauncherSetter(launcherFiles[i].getName(), launcherId, launcherVersion, configSpec, result);
			}
		}
		touchpointData.put("install", configurationData); //$NON-NLS-1$
		String unConfigurationData = "cleanupzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("uninstall", unConfigurationData); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		IInstallableUnit unit = MetadataFactory.createInstallableUnit(cu);
		result.addIU(unit, IPublisherResult.ROOT);
		//The Product Query will need to include the launcher CU fragments as a workaround to bug 218890
		result.addFragment(launcherIdPrefix, unit);

		//Create the artifact descriptor
		IArtifactDescriptor descriptor = MetadataGeneratorHelper.createArtifactDescriptor(key, root, false, true);
		publishArtifact(descriptor, root.listFiles(), destination, false, true);
	}

	/*
	 * For each platform, generate a CU containing the information for the config.ini
	 */
	protected void generateProductConfigCUs(IPublisherResult result) {
		for (Iterator iterator = result.getConfigData().keySet().iterator(); iterator.hasNext();) {
			String launcherConfig = (String) iterator.next();
			ConfigData data = (ConfigData) result.getConfigData().get(launcherConfig);

			InstallableUnitDescription cu = new MetadataFactory.InstallableUnitDescription();
			String configUnitId = info.getFlavor() + product.getId() + ".config." + createIdString(launcherConfig); //$NON-NLS-1$
			Version cuVersion = new Version(product.getVersion());
			cu.setId(configUnitId);
			cu.setVersion(cuVersion);
			cu.setFilter(createFilterSpec(launcherConfig));

			ProvidedCapability productConfigCapability = MetadataFactory.createProvidedCapability(info.getFlavor() + product.getId(), product.getId() + ".config", Version.emptyVersion); //$NON-NLS-1$
			ProvidedCapability selfCapability = MetadataGeneratorHelper.createSelfCapability(configUnitId, cuVersion);
			cu.setCapabilities(new ProvidedCapability[] {selfCapability, productConfigCapability});

			cu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_OSGI);
			Map touchpointData = new HashMap();
			String[] dataStrings = getConfigurationStrings(data);
			touchpointData.put("configure", dataStrings[0]); //$NON-NLS-1$
			touchpointData.put("unconfigure", dataStrings[1]); //$NON-NLS-1$
			cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

			result.addIU(MetadataFactory.createInstallableUnit(cu), IPublisherResult.ROOT);
		}
	}

	/* 
	 * For the given platform (ws, os, arch) generate the CU that will populate the product.ini file 
	 */
	protected void generateProductIniCU(String configSpec, String version, IPublisherResult result) {
		if (product == null)
			return;

		//attempt to merge arguments from the launcher data and the product file
		Set jvmArgs = new LinkedHashSet();
		Set progArgs = new LinkedHashSet();
		LauncherData launcherData = info.getLauncherData();
		if (launcherData != null) {
			jvmArgs.addAll(Arrays.asList(launcherData.getJvmArgs()));
			progArgs.addAll(Arrays.asList(launcherData.getProgramArgs()));
		}
		String[] config = parseConfigSpec(configSpec);
		progArgs.addAll(Arrays.asList(getArrayFromString(product.getProgramArguments(config[1]), " "))); //$NON-NLS-1$
		jvmArgs.addAll(Arrays.asList(getArrayFromString(product.getVMArguments(config[1]), " "))); //$NON-NLS-1$

		String[] dataStrings = getLauncherConfigStrings((String[]) jvmArgs.toArray(new String[jvmArgs.size()]), (String[]) progArgs.toArray(new String[progArgs.size()]));
		String configurationData = dataStrings[0];
		String unconfigurationData = dataStrings[1];

		if (configurationData.length() == 0)
			return;

		InstallableUnitDescription cu = new MetadataFactory.InstallableUnitDescription();
		String configUnitId = info.getFlavor() + product.getId() + ".ini." + createIdString(configSpec); //$NON-NLS-1$
		Version cuVersion = new Version(version);
		cu.setId(configUnitId);
		cu.setVersion(cuVersion);
		cu.setFilter(createFilterSpec(configSpec));

		ProvidedCapability productIniCapability = MetadataFactory.createProvidedCapability(info.getFlavor() + product.getId(), product.getId() + ".ini", Version.emptyVersion); //$NON-NLS-1$
		ProvidedCapability selfCapability = MetadataGeneratorHelper.createSelfCapability(configUnitId, cuVersion);
		cu.setCapabilities(new ProvidedCapability[] {selfCapability, productIniCapability});

		cu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_OSGI);
		Map touchpointData = new HashMap();
		touchpointData.put("configure", configurationData); //$NON-NLS-1$
		touchpointData.put("unconfigure", unconfigurationData); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		result.addIU(MetadataFactory.createInstallableUnit(cu), IPublisherResult.ROOT);

	}

	protected void generateFeatureIUs(Feature[] features, IPublisherResult result, IArtifactRepository destination) {
		Map categoriesToFeatureIUs = new HashMap();
		Map featuresToCategories = getFeatureToCategoryMappings(info.getSiteLocation());
		//Build Feature IUs, and add them to any corresponding categories
		for (int i = 0; i < features.length; i++) {
			Feature feature = features[i];

			// create the basic feature IU with all the children
			String location = feature.getLocation();
			boolean isExploded = (location.endsWith(".jar") ? false : true); //$NON-NLS-1$
			IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureJarIU(feature, null, isExploded, null);

			// add all the artifacts associated with the feature
			IArtifactKey[] artifacts = featureIU.getArtifacts();
			for (int arti = 0; arti < artifacts.length; arti++) {
				IArtifactDescriptor ad = MetadataGeneratorHelper.createArtifactDescriptor(artifacts[arti], new File(location), true, false);
				if (isExploded)
					publishArtifact(ad, new File(location).listFiles(), destination, false, true);
				else
					publishArtifact(ad, new File[] {new File(location)}, destination, true, true);
			}

			// create the associated group and register the feature and group in the result.
			IInstallableUnit generated = MetadataGeneratorHelper.createGroupIU(feature, featureIU);
			result.addIU(generated, IPublisherResult.ROOT);
			result.addIU(featureIU, IPublisherResult.ROOT);

			// Assign the feature to categories if applicable.  otherwise add it to the root category
			Set categories = getCategories(feature, featuresToCategories);
			if (categories != null) {
				for (Iterator it = categories.iterator(); it.hasNext();) {
					SiteCategory category = (SiteCategory) it.next();
					Set featureIUs = (Set) categoriesToFeatureIUs.get(category);
					if (featureIUs == null) {
						featureIUs = new HashSet();
						categoriesToFeatureIUs.put(category, featureIUs);
					}
					featureIUs.add(generated);
				}
			} else {
				rootCategory.add(generated);
			}
		}
		generateCategoryIUs(categoriesToFeatureIUs, result);
	}

	protected void generateNativeIUs(File executableLocation, IPublisherResult result, IArtifactRepository destination) {
		//generate data for JRE
		File jreLocation = info.getJRELocation();
		IArtifactDescriptor artifact = MetadataGeneratorHelper.createJREData(jreLocation, result);
		publishArtifact(artifact, new File[] {jreLocation}, destination, false, true);

		if (info.getLauncherConfig() != null) {
			String configSpec = info.getLauncherConfig();
			String version = "1.0.0"; //$NON-NLS-1$
			if (product != null && !product.getVersion().equals("0.0.0")) //$NON-NLS-1$
				version = product.getVersion();
			generateExecutableIUs(configSpec, version, executableLocation.getParentFile(), result, destination);
			generateProductIniCU(configSpec, version, result);
			return;
		}

		//If the executable feature is present, use it to generate IUs for launchers
		if (generateExecutableFeatureIUs(result, destination) || executableLocation == null)
			return;

		//generate data for executable launcher
		artifact = MetadataGeneratorHelper.createLauncherIU(executableLocation, info.getFlavor(), result);
		File[] launcherFiles = null;
		//hard-coded name is ok, since console launcher is not branded, and appears on Windows only
		File consoleLauncher = new File(executableLocation.getParentFile(), "eclipsec.exe"); //$NON-NLS-1$
		if (consoleLauncher.exists())
			launcherFiles = new File[] {executableLocation, consoleLauncher};
		else
			launcherFiles = new File[] {executableLocation};
		publishArtifact(artifact, launcherFiles, destination, false, true);
	}

	protected void generateRootIU(IPublisherResult result, String rootIUId, String rootIUVersion) {
		IInstallableUnit rootIU = null;

		if (info.getProduct() != null)
			rootIU = createProductIU(result);
		else if (rootIUId != null)
			rootIU = createTopLevelIU(result, rootIUId, rootIUVersion);

		if (rootIU == null)
			return;

		result.addIU(rootIU, IPublisherResult.NON_ROOT);
		result.addIU(generateDefaultCategory(rootIU, rootCategory), IPublisherResult.NON_ROOT);
	}

	protected BundleDescription[] getBundleDescriptions(File[] bundleLocations) {
		if (bundleLocations == null)
			return new BundleDescription[0];
		boolean addSimpleConfigurator = false;
		boolean scIn = false;
		for (int i = 0; i < bundleLocations.length; i++) {
			if (!addSimpleConfigurator)
				addSimpleConfigurator = bundleLocations[i].toString().indexOf(ORG_ECLIPSE_UPDATE_CONFIGURATOR) > 0;
			if (!scIn) {
				scIn = bundleLocations[i].toString().indexOf(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR) > 0;
				if (scIn)
					break;
			}
		}
		if (scIn)
			addSimpleConfigurator = false;
		BundleDescription[] result = new BundleDescription[bundleLocations.length + (addSimpleConfigurator ? 1 : 0)];
		BundleDescriptionFactory factory = getBundleFactory();
		for (int i = 0; i < bundleLocations.length; i++) {
			result[i] = factory.getBundleDescription(bundleLocations[i]);
		}
		if (addSimpleConfigurator) {
			//Add simple configurator to the list of bundles
			try {
				File location = new File(FileLocator.toFileURL(Activator.getContext().getBundle().getEntry(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR + ".jar")).getFile()); //$NON-NLS-1$
				result[result.length - 1] = factory.getBundleDescription(location);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	protected BundleDescriptionFactory getBundleFactory() {
		return new BundleDescriptionFactory(stateObjectFactory, null);
	}

	/**
	 * Returns the categories corresponding to the given feature, or null if there
	 * are no applicable categories.
	 * @param feature The feature to return categories for
	 * @param featuresToCategories A map of SiteFeature->Set<SiteCategory>
	 * @return A Set<SiteCategory> of the categories corresponding to the feature, or <code>null</code>
	 */
	protected Set getCategories(Feature feature, Map featuresToCategories) {
		//find the SiteFeature corresponding to the given feature
		for (Iterator it = featuresToCategories.keySet().iterator(); it.hasNext();) {
			SiteFeature siteFeature = (SiteFeature) it.next();
			if (siteFeature.getFeatureIdentifier().equals(feature.getId()) && siteFeature.getFeatureVersion().equals(feature.getVersion()))
				return (Set) featuresToCategories.get(siteFeature);
		}
		return null;
	}

	protected Feature[] getFeatures(File[] locations) {
		ArrayList result = new ArrayList(locations.length);
		for (int i = 0; i < locations.length; i++) {
			Feature feature = new FeatureParser().parse(locations[i]);
			if (feature != null) {
				feature.setLocation(locations[i].getAbsolutePath());
				result.add(feature);
			}
		}
		return (Feature[]) result.toArray(new Feature[result.size()]);
	}

	private Feature[] getFeatures(File folder) {
		if (folder == null || !folder.exists())
			return new Feature[0];
		File[] locations = folder.listFiles();
		return getFeatures(locations);
	}

	/**
	 * Computes the mapping of features to categories as defined in the site.xml,
	 * if available. Returns an empty map if there is not site.xml, or no categories.
	 * @return A map of SiteFeature -> Set<SiteCategory>.
	 */
	protected Map getFeatureToCategoryMappings(URL siteLocation) {
		HashMap mappings = new HashMap();
		if (siteLocation == null)
			return mappings;
		InputStream input;
		SiteModel site = null;
		try {
			input = new BufferedInputStream(siteLocation.openStream());
			site = new DefaultSiteParser().parse(input);
		} catch (FileNotFoundException e) {
			//don't complain if the update site is not present
		} catch (Exception e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.exception_errorParsingUpdateSite, siteLocation), e));
		}
		if (site == null)
			return mappings;

		//copy mirror information from update site to p2 repositories
		String mirrors = site.getMirrorsURL();
		if (mirrors != null) {
			//remove site.xml file reference
			int index = mirrors.indexOf("site.xml"); //$NON-NLS-1$
			if (index != -1)
				mirrors = mirrors.substring(0, index) + mirrors.substring(index + 9);
			info.getMetadataRepository().setProperty(IRepository.PROP_MIRRORS_URL, mirrors);
			info.getArtifactRepository().setProperty(IRepository.PROP_MIRRORS_URL, mirrors);
		}

		SiteFeature[] features = site.getFeatures();
		for (int i = 0; i < features.length; i++) {
			//add a mapping for each category this feature belongs to
			String[] categoryNames = features[i].getCategoryNames();
			for (int j = 0; j < categoryNames.length; j++) {
				SiteCategory category = site.getCategory(categoryNames[j]);
				if (category != null) {
					Set categories = (Set) mappings.get(features[i]);
					if (categories == null) {
						categories = new HashSet();
						mappings.put(features[i], categories);
					}
					categories.add(category);
				}
			}
		}
		return mappings;
	}

	protected IGeneratorInfo getGeneratorInfo() {
		return info;
	}

	/**
	 * @TODO This method is a temporary hack to rename the launcher.exe files
	 * to eclipse.exe (or "launcher" to "eclipse"). Eventually we will either hand-craft
	 * metadata/artifacts for launchers, or alter the delta pack to contain eclipse-branded
	 * launchers.
	 */
	private void mungeLauncherFileNames(File root) {
		if (root.isDirectory()) {
			File[] children = root.listFiles();
			for (int i = 0; i < children.length; i++) {
				mungeLauncherFileNames(children[i]);
			}
		} else if (root.isFile()) {
			if (root.getName().equals("launcher")) //$NON-NLS-1$
				root.renameTo(new File(root.getParentFile(), "eclipse")); //$NON-NLS-1$
			else if (root.getName().equals("launcher.exe")) //$NON-NLS-1$
				root.renameTo(new File(root.getParentFile(), "eclipse.exe")); //$NON-NLS-1$
		}
	}

	// Put the artifact on the server
	protected void publishArtifact(IArtifactDescriptor descriptor, File[] files, IArtifactRepository destination, boolean asIs, boolean includeRoot) {
		if (descriptor == null || destination == null)
			return;
		if (!info.publishArtifacts()) {
			destination.addDescriptor(descriptor);
			return;
		}
		if (asIs && files.length == 1) {
			try {
				if (!destination.contains(descriptor)) {
					OutputStream output = new BufferedOutputStream(destination.getOutputStream(descriptor));
					FileUtils.copyStream(new BufferedInputStream(new FileInputStream(files[0])), true, output, true);
				}
			} catch (ProvisionException e) {
				LogHelper.log(e.getStatus());
			} catch (IOException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error publishing artifacts", e)); //$NON-NLS-1$
			}
		} else {
			File tempFile = null;
			try {
				tempFile = File.createTempFile("p2.generator", ""); //$NON-NLS-1$ //$NON-NLS-2$
				FileUtils.zip(files, tempFile, includeRoot);
				if (!destination.contains(descriptor)) {
					OutputStream output = new BufferedOutputStream(destination.getOutputStream(descriptor));
					FileUtils.copyStream(new BufferedInputStream(new FileInputStream(tempFile)), true, output, true);
				}
			} catch (ProvisionException e) {
				LogHelper.log(e.getStatus());
			} catch (IOException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error publishing artifacts", e)); //$NON-NLS-1$
			} finally {
				if (tempFile != null)
					tempFile.delete();
			}
		}
	}

	public void setGenerateRootIU(boolean generateRootIU) {
		this.generateRootIU = generateRootIU;
	}
}
