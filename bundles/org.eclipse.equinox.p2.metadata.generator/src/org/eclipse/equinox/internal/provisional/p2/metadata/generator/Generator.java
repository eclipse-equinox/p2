/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata.generator;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.frameworkadmin.equinox.EquinoxConstants;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.generator.*;
import org.eclipse.equinox.internal.p2.metadata.generator.Messages;
import org.eclipse.equinox.internal.p2.metadata.generator.features.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Filter;

public class Generator {
	/**
	 * Captures the output of an execution of the generator.
	 */
	public static class GeneratorResult {
		public static final String CONFIGURATION_CUS = "CONFIGURATION_CUS"; //$NON-NLS-1$

		final public Map pluginShape = new HashMap();

		/**
		 * The set of generated IUs that will be children of the root IU
		 */
		final public Set rootIUs = new HashSet();
		/**
		 * The set of generated IUs that will not be children of the root IU
		 */
		final public Set nonRootIUs = new HashSet();

		/**
		 * Map of symbolic name to a set of generated CUs for that IU
		 */
		final public Map configurationIUs = new HashMap();

		/**
		 * Map launcherConfig to config.ini ConfigData
		 */
		final public Map configData = new HashMap();

		/**
		 * Returns all IUs generated during this execution of the generator.
		 */
		public Set allGeneratedIUs() {
			HashSet all = new HashSet();
			all.addAll(rootIUs);
			all.addAll(nonRootIUs);
			return all;
		}

		/**
		 * Returns the IU in this result with the given id.
		 */
		public IInstallableUnit getInstallableUnit(String id) {
			for (Iterator iterator = rootIUs.iterator(); iterator.hasNext();) {
				IInstallableUnit tmp = (IInstallableUnit) iterator.next();
				if (tmp.getId().equals(id))
					return tmp;
			}
			for (Iterator iterator = nonRootIUs.iterator(); iterator.hasNext();) {
				IInstallableUnit tmp = (IInstallableUnit) iterator.next();
				if (tmp.getId().equals(id))
					return tmp;
			}
			return null;

		}

		public Map getPluginShapeInfo() {
			return pluginShape;
		}
	}

	private static final String ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR = "org.eclipse.equinox.simpleconfigurator"; //$NON-NLS-1$
	private static final String ORG_ECLIPSE_UPDATE_CONFIGURATOR = "org.eclipse.update.configurator"; //$NON-NLS-1$
	private static final String ORG_ECLIPSE_EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher"; //$NON-NLS-1$

	private static final String PRODUCT_CONFIG_SUFFIX = ".config"; //$NON-NLS-1$
	private static final String PRODUCT_INI_SUFFIX = ".ini"; //$NON-NLS-1$
	private static final String PRODUCT_LAUCHER_SUFFIX = ".launcher"; //$NON-NLS-1$
	private static final String CONFIG_ANY = "ANY"; //$NON-NLS-1$

	private static final String PROTOCOL_FILE = "file"; //$NON-NLS-1$

	protected final IGeneratorInfo info;

	private GeneratorResult incrementalResult = null;
	private ProductFile productFile = null;
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

	public static String[] parseConfigSpec(String config) {
		String[] parsed = getArrayFromString(config, "_"); //$NON-NLS-1$
		for (int i = 0; i < parsed.length; i++) {
			if (parsed[i].equals("*")) //$NON-NLS-1$
				parsed[i] = "ANY"; //$NON-NLS-1$
		}
		if (parsed.length > 3) {
			String[] adjusted = new String[] {parsed[0], parsed[1], parsed[2] + '_' + parsed[3]};
			return adjusted;
		}
		return parsed;
	}

	public Generator(IGeneratorInfo infoProvider) {
		this.info = infoProvider;
		// TODO need to figure a better way of configuring the generator...
		PlatformAdmin platformAdmin = (PlatformAdmin) ServiceHelper.getService(Activator.getContext(), PlatformAdmin.class.getName());
		if (platformAdmin != null) {
			stateObjectFactory = platformAdmin.getFactory();
		}
	}

	public void setIncrementalResult(GeneratorResult result) {
		this.incrementalResult = result;
	}

	private String getProductVersion() {
		String version = "1.0.0"; //$NON-NLS-1$
		if (productFile != null && !productFile.getVersion().equals("0.0.0")) //$NON-NLS-1$
			version = productFile.getVersion();
		else if (!info.getRootVersion().equals("0.0.0")) //$NON-NLS-1$
			version = info.getRootVersion();
		return version;
	}

	/** 
	 * @deprecated moved to ProductAction
	 */
	protected IInstallableUnit createProductIU(GeneratorResult result) {
		generateProductConfigCUs(result);

		GeneratorResult productContents = new GeneratorResult();

		ProductQuery productQuery = new ProductQuery(productFile, info.getFlavor(), result.configurationIUs, info.getVersionAdvice());
		IQuery query = QueryUtil.createLatestQuery(productQuery);
		IQueryResult queryResult = info.getMetadataRepository().query(query, null);
		for (Iterator iterator = queryResult.iterator(); iterator.hasNext();) {
			productContents.rootIUs.add(iterator.next());
		}

		String version = getProductVersion();
		VersionRange range = new VersionRange(Version.create(version), true, Version.create(version), true);
		ArrayList requires = new ArrayList(1);
		requires.add(MetadataFactory.createRequiredCapability(info.getFlavor() + productFile.getId(), productFile.getId() + PRODUCT_LAUCHER_SUFFIX, range, null, false, true));
		requires.add(MetadataFactory.createRequiredCapability(info.getFlavor() + productFile.getId(), productFile.getId() + PRODUCT_INI_SUFFIX, range, null, false, false));
		requires.add(MetadataFactory.createRequiredCapability(info.getFlavor() + productFile.getId(), productFile.getId() + PRODUCT_CONFIG_SUFFIX, range, null, false, false));

		//default CUs		
		requires.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, MetadataGeneratorHelper.createDefaultConfigUnitId(MetadataGeneratorHelper.OSGI_BUNDLE_CLASSIFIER, info.getFlavor()), VersionRange.emptyRange, null, false, false));
		requires.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, MetadataGeneratorHelper.createDefaultConfigUnitId("source", info.getFlavor()), VersionRange.emptyRange, null, false, false)); //$NON-NLS-1$
		if (productFile.useFeatures())
			requires.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, MetadataGeneratorHelper.createDefaultConfigUnitId(MetadataGeneratorHelper.ECLIPSE_FEATURE_CLASSIFIER, info.getFlavor()), VersionRange.emptyRange, MetadataGeneratorHelper.INSTALL_FEATURES_FILTER, true, false));

		InstallableUnitDescription root = createTopLevelIUDescription(productContents, productFile.getId(), version, productFile.getProductName(), requires, false);
		return MetadataFactory.createInstallableUnit(root);
	}

	/**
	 * @deprecated moved to RootIUAction
	 */
	protected IInstallableUnit createTopLevelIU(GeneratorResult result, String configurationIdentification, String configurationVersion) {
		// TODO, bit of a hack but for now set the name of the IU to the ID.
		InstallableUnitDescription root = createTopLevelIUDescription(result, configurationIdentification, configurationVersion, configurationIdentification, null, true);
		return MetadataFactory.createInstallableUnit(root);
	}

	/**
	 * @deprecated moved to RootIUAction
	 */
	protected InstallableUnitDescription createTopLevelIUDescription(GeneratorResult result, String configurationIdentification, String configurationVersion, String configurationName, List requires, boolean configureLauncherData) {
		InstallableUnitDescription root = new MetadataFactory.InstallableUnitDescription();
		root.setSingleton(true);
		root.setId(configurationIdentification);
		root.setVersion(Version.create(configurationVersion));
		root.setProperty(IInstallableUnit.PROP_NAME, configurationName);

		ArrayList reqsConfigurationUnits = new ArrayList(result.rootIUs.size());
		for (Iterator iterator = result.rootIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
			//			boolean isOptional = checkOptionalRootDependency(iu);
			reqsConfigurationUnits.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, iu.getFilter(), false, false));
		}
		if (requires != null)
			reqsConfigurationUnits.addAll(requires);
		root.setRequiredCapabilities((IRequirement[]) reqsConfigurationUnits.toArray(new IRequirement[reqsConfigurationUnits.size()]));
		root.setArtifacts(new IArtifactKey[0]);

		root.setProperty("lineUp", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		root.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(configurationIdentification, VersionRange.emptyRange, IUpdateDescriptor.NORMAL, null));
		root.setProperty(InstallableUnitDescription.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		root.setCapabilities(new IProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(configurationIdentification, Version.create(configurationVersion))});
		root.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_OSGI);
		Map touchpointData = new HashMap();

		// Publisher refactor - the configdata stuff moved to a distinct IU added by the ConfigCUsAction
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
		//look for additional touchpoint instructions in a p2.inf file
		final String productFileLocation = info.getProductFile();
		if (productFileLocation != null) {
			File productFilePath = new File(productFileLocation);
			if (productFilePath.exists()) {
				Map advice = MetadataGeneratorHelper.getBundleAdvice(productFilePath.getParent(), "p2.inf");//$NON-NLS-1$
				if (advice != null)
					MetadataGeneratorHelper.mergeInstructionsAdvice(touchpointData, advice);
			}
		}

		root.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		return root;
	}

	/**
	 * @deprecated moved to ConfigCUsAction
	 */
	private String[] getConfigurationStrings(ConfigData configData) {
		String configurationData = ""; //$NON-NLS-1$
		String unconfigurationData = ""; //$NON-NLS-1$
		for (Iterator iterator = configData.getProperties().entrySet().iterator(); iterator.hasNext();) {
			Entry aProperty = (Entry) iterator.next();
			String key = ((String) aProperty.getKey());
			if (key.equals("osgi.frameworkClassPath") || key.equals("osgi.framework") || key.equals("osgi.bundles") || key.equals("eof")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				continue;
			configurationData += "setProgramProperty(propName:" + key + ", propValue:" + ((String) aProperty.getValue()) + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			unconfigurationData += "setProgramProperty(propName:" + key + ", propValue:);"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		for (Iterator iterator = configData.getProperties().entrySet().iterator(); iterator.hasNext();) {
			Entry aProperty = (Entry) iterator.next();
			String key = ((String) aProperty.getKey());
			if (key.equals("osgi.frameworkClassPath") || key.equals("osgi.framework") || key.equals("osgi.bundles") || key.equals("eof")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				continue;
			configurationData += "setProgramProperty(propName:" + key + ", propValue:" + ((String) aProperty.getValue()) + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			unconfigurationData += "setProgramProperty(propName:" + key + ", propValue:);"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		return new String[] {configurationData, unconfigurationData};
	}

	/**
	 * @deprecated moved to ConfigCUsAction
	 */
	private String[] getLauncherConfigStrings(final String[] jvmArgs, final String[] programArgs) {
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
		GeneratorResult result = incrementalResult != null ? incrementalResult : new GeneratorResult();

		if (info.getProductFile() != null) {
			try {
				productFile = new ProductFile(info.getProductFile(), null);
			} catch (Exception e) {
				//TODO
			}
		}

		Feature[] features = getFeatures(info.getFeaturesLocation());
		generateFeatureIUs(features, result, info.getArtifactRepository());

		BundleDescription[] bundles = getBundleDescriptions(info.getBundleLocations());
		generateBundleIUs(bundles, result, info.getArtifactRepository());

		generateNativeIUs(info.getExecutableLocation(), result, info.getArtifactRepository());

		generateConfigIUs(result);

		if (info.addDefaultIUs())
			generateDefaultConfigIU(result.rootIUs);

		if (generateRootIU)
			generateRootIU(result, info.getRootId(), info.getRootVersion());

		//		persistence.setMappingRules(info.getMappingRules() == null ? defaultMappingRules : info.getMappingRules());
		//		if (info.publishArtifacts() || info.publishArtifactRepository()) {
		//			persistence.saveArtifactRepository();
		//		}
		IMetadataRepository metadataRepository = info.getMetadataRepository();
		if (metadataRepository != null) {
			Set allGeneratedUnits = result.allGeneratedIUs();
			metadataRepository.addInstallableUnits(allGeneratedUnits);
		}

		return Status.OK_STATUS;
	}

	/**
	 * @deprecated moved to BundlesAction
	 */
	protected void generateBundleIUs(BundleDescription[] bundles, GeneratorResult result, IArtifactRepository destination) {
		// Computing the path for localized property files in a NL fragment bundle
		// requires the BUNDLE_LOCALIZATION property from the manifest of the host bundle,
		// so a first pass is done over all the bundles to cache this value as well as the tags
		// from the manifest for the localizable properties.
		final int CACHE_PHASE = 0;
		final int GENERATE_PHASE = 1;
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
						String format = (String) result.getPluginShapeInfo().get(bd.getSymbolicName() + '_' + bd.getVersion());
						if (format == null)
							format = (String) bundleManifest.get(BundleDescriptionFactory.BUNDLE_FILE_KEY);
						boolean isDir = (format != null && format.equals(BundleDescriptionFactory.DIR) ? true : false);

						IArtifactKey key = new ArtifactKey(MetadataGeneratorHelper.OSGI_BUNDLE_CLASSIFIER, bd.getSymbolicName(), Version.create(bd.getVersion().toString()));
						IArtifactDescriptor ad = MetadataGeneratorHelper.createArtifactDescriptor(key, new File(bd.getLocation()), true, false);
						((ArtifactDescriptor) ad).setProperty(IArtifactDescriptor.DOWNLOAD_CONTENTTYPE, IArtifactDescriptor.TYPE_ZIP);
						File bundleFile = new File(bd.getLocation());
						if (bundleFile.isDirectory())
							publishArtifact(ad, bundleFile.listFiles(), destination, false, bundleFile);
						else
							publishArtifact(ad, new File[] {bundleFile}, destination, true);
						if (info.reuseExistingPack200Files() && !info.publishArtifacts()) {
							File packFile = new Path(bd.getLocation()).addFileExtension("pack.gz").toFile(); //$NON-NLS-1$
							if (packFile.exists()) {
								IArtifactDescriptor ad200 = MetadataGeneratorHelper.createPack200ArtifactDescriptor(key, packFile, ad.getProperty(IArtifactDescriptor.ARTIFACT_SIZE));
								publishArtifact(ad200, new File[] {packFile}, destination, true);
							}
						}

						IInstallableUnit bundleIU = MetadataGeneratorHelper.createBundleIU(bd, bundleManifest, isDir, key, true);

						if (isFragment(bd)) {
							// TODO: Can NL fragments be multi-host?  What special handling
							//		 is required for multi-host fragments in general?
							String hostId = bd.getHost().getName();
							String hostKey = makeSimpleKey(hostId);
							String[] cachedValues = (String[]) bundleLocalizationMap.get(hostKey);

							if (cachedValues != null) {
								MetadataGeneratorHelper.createHostLocalizationFragment(bundleIU, bd, hostId, cachedValues, localizationIUs);
							}
						}

						result.rootIUs.add(bundleIU);
						result.nonRootIUs.addAll(localizationIUs);
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
	 * @deprecated moved to SiteXMLAction
	 */
	protected void generateCategoryIUs(Map categoriesToFeatures, GeneratorResult result) {
		for (Iterator it = categoriesToFeatures.keySet().iterator(); it.hasNext();) {
			SiteCategory category = (SiteCategory) it.next();
			result.nonRootIUs.add(MetadataGeneratorHelper.createCategoryIU(category, (Set) categoriesToFeatures.get(category), null));
		}
	}

	/**
	 * @deprecated moved to ConfigCUsAction
	 */
	private void storeConfigData(GeneratorResult result) {
		if (result.configData.containsKey(info.getLauncherConfig()))
			return; //been here, done this

		LauncherData launcherData = info.getLauncherData();
		if (launcherData == null)
			return;

		File fwConfigFile = new File(launcherData.getFwConfigLocation(), EquinoxConstants.CONFIG_INI);
		if (fwConfigFile.exists()) {
			if (info instanceof EclipseInstallGeneratorInfoProvider) {
				((EclipseInstallGeneratorInfoProvider) info).loadConfigData(fwConfigFile);
				ConfigData data = info.getConfigData();
				result.configData.put(info.getLauncherConfig(), data);
			}
		}
	}

	/**
	 * @deprecated moved to ConfigCUsAction
	 */
	protected GeneratorBundleInfo createGeneratorBundleInfo(BundleInfo bundleInfo, GeneratorResult result) {
		if (bundleInfo.getLocation() != null)
			return new GeneratorBundleInfo(bundleInfo);

		String name = bundleInfo.getSymbolicName();

		//easy case: do we have a matching IU?
		IInstallableUnit iu = result.getInstallableUnit(name);
		if (iu != null) {
			bundleInfo.setVersion(iu.getVersion().toString());
			return new GeneratorBundleInfo(bundleInfo);
		}

		//harder: try id_version
		int i = name.indexOf('_');
		while (i > -1) {
			Version version = null;
			try {
				version = Version.create(name.substring(i));
				bundleInfo.setSymbolicName(name.substring(0, i));
				bundleInfo.setVersion(version.toString());
				return new GeneratorBundleInfo(bundleInfo);
			} catch (IllegalArgumentException e) {
				// the '_' found was probably part of the symbolic id
				i = name.indexOf('_', i);
			}
		}

		//Query the repo
		IQuery query = QueryUtil.createIUQuery(name);
		Iterator matches = info.getMetadataRepository().query(query, null).iterator();
		//pick the newest match
		IInstallableUnit newest = null;
		while (matches.hasNext()) {
			IInstallableUnit candidate = (IInstallableUnit) matches.next();
			if (newest == null || (newest.getVersion().compareTo(candidate.getVersion()) < 0))
				newest = candidate;
		}
		if (newest != null) {
			bundleInfo.setVersion(newest.getVersion().toString());
			return new GeneratorBundleInfo(bundleInfo);
		}

		return null;
	}

	/** 
	 * @deprecated moved to ConfigCUsAction
	 */
	protected void generateBundleConfigIUs(BundleInfo[] infos, GeneratorResult result, String launcherConfig, int defaultStartLevel) {
		if (infos == null)
			return;

		String cuIdPrefix = ""; //$NON-NLS-1$
		String filter = null;
		if (launcherConfig != null) {
			//launcher config is os_ws_arch, we want suffix ws.os.arch
			String[] config = parseConfigSpec(launcherConfig);
			cuIdPrefix = config[1] + '.' + config[0] + '.' + config[2];

			filter = "(& (osgi.ws=" + config[1] + ") (osgi.os=" + config[0] + ") (osgi.arch=" + config[2] + "))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		List allCUs = new ArrayList();
		for (int i = 0; i < infos.length; i++) {
			GeneratorBundleInfo bundle = createGeneratorBundleInfo(infos[i], result);
			if (bundle == null)
				continue;

			if (bundle.getSymbolicName().equals(ORG_ECLIPSE_EQUINOX_LAUNCHER)) {
				bundle = EclipseInstallGeneratorInfoProvider.createLauncher();
			} else if (bundle.getSymbolicName().startsWith(ORG_ECLIPSE_EQUINOX_LAUNCHER + '.')) {
				//launcher fragments will be handled by generateDefaultConfigIU(Set) for --launcher.library.
				//they don't need to be started so skip them here to avoid having to merge config commands
				continue;
			}
			if (bundle.getSymbolicName().equals(ORG_ECLIPSE_UPDATE_CONFIGURATOR)) {
				bundle.setStartLevel(BundleInfo.NO_LEVEL);
				bundle.setMarkedAsStarted(false);
				bundle.setSpecialConfigCommands("setProgramProperty(propName:org.eclipse.update.reconcile, propValue:false);"); //$NON-NLS-1$
				bundle.setSpecialUnconfigCommands("setProgramProperty(propName:org.eclipse.update.reconcile, propValue:);"); //$NON-NLS-1$
			} else if ((bundle.getStartLevel() == BundleInfo.NO_LEVEL || bundle.getStartLevel() == defaultStartLevel) && !bundle.isMarkedAsStarted()) {
				// this bundle does not require any particular configuration, the plug-in default IU will handle installing it
				continue;
			}

			IInstallableUnit cu = MetadataGeneratorHelper.createBundleConfigurationUnit(bundle.getSymbolicName(), Version.create(bundle.getVersion()), false, bundle, info.getFlavor() + cuIdPrefix, filter);
			if (cu != null) {
				allCUs.add(cu);
				result.rootIUs.add(cu);
				String key = (productFile != null && productFile.useFeatures()) ? GeneratorResult.CONFIGURATION_CUS : bundle.getSymbolicName();
				if (result.configurationIUs.containsKey(key)) {
					((Set) result.configurationIUs.get(key)).add(cu);
				} else {
					Set set = new HashSet();
					set.add(cu);
					result.configurationIUs.put(key, set);
				}
			}
		}
		IMetadataRepository metadataRepository = info.getMetadataRepository();
		if (metadataRepository != null && !allCUs.isEmpty()) {
			// Product Query will run against the repo later in createProductIU, make sure these CUs are in before then
			metadataRepository.addInstallableUnits(allCUs);
		}

	}

	/**
	 * @deprecated moved to ConfigCUsAction (and perhaps a couple other places...)
	 */
	protected void generateConfigIUs(GeneratorResult result) {
		ConfigData data = info.getConfigData();
		if ((data == null || data.getBundles().length == 0) && info.getLauncherConfig() != null) {
			//We have the config.ini but not necessarily all the needed bundle IUs, remember for later
			storeConfigData(result);
		} else if (data != null) {
			// generation against an eclipse install (config.ini + bundles)
			generateBundleConfigIUs(data.getBundles(), result, info.getLauncherConfig(), data.getInitialBundleStartLevel());
		} else if (result.configData.size() > 0 && generateRootIU) {
			// generation from remembered config.ini's
			// we have N platforms, generate a CU for each
			// TODO try and find common properties across platforms
			for (Iterator iterator = result.configData.keySet().iterator(); iterator.hasNext();) {
				String launcherConfig = (String) iterator.next();
				data = (ConfigData) result.configData.get(launcherConfig);
				generateBundleConfigIUs(data.getBundles(), result, launcherConfig, data.getInitialBundleStartLevel());
			}
		}

		List bundleInfoList = new ArrayList();
		List defaults = new ArrayList();
		if (info.addDefaultIUs())
			bundleInfoList.addAll(info.getDefaultIUs(result.rootIUs));

		bundleInfoList.addAll(info.getOtherIUs());

		for (Iterator iterator = bundleInfoList.iterator(); iterator.hasNext();) {
			GeneratorBundleInfo bundle = (GeneratorBundleInfo) iterator.next();
			IInstallableUnit configuredIU = result.getInstallableUnit(bundle.getSymbolicName());
			if (configuredIU == null) {
				if (!generateRootIU && data == null)
					continue;
				IQuery query = QueryUtil.createIUQuery(bundle.getSymbolicName());
				IMetadataRepository metadataRepository = info.getMetadataRepository();
				if (metadataRepository == null)
					continue;
				Iterator matches = metadataRepository.query(query, null).iterator();
				//pick the newest match
				IInstallableUnit newest = null;
				while (matches.hasNext()) {
					IInstallableUnit candidate = (IInstallableUnit) matches.next();
					if (newest == null || (newest.getVersion().compareTo(candidate.getVersion()) < 0))
						newest = candidate;
				}
				if (newest != null) {
					configuredIU = newest;
				} else {
					continue;
				}
			}
			bundle.setVersion(configuredIU.getVersion().toString());
			Filter filter = configuredIU == null ? null : configuredIU.getFilter();
			IInstallableUnit cu = MetadataGeneratorHelper.createBundleConfigurationUnit(bundle.getSymbolicName(), Version.create(bundle.getVersion()), false, bundle, info.getFlavor(), filter == null ? null : filter.toString());
			//the configuration unit should share the same platform filter as the IU being configured.
			if (cu != null) {
				result.rootIUs.add(cu);
				defaults.add(cu);
			}
			String key = null;
			if (productFile != null && productFile.useFeatures())
				key = GeneratorResult.CONFIGURATION_CUS;
			else if (bundle.getSymbolicName().startsWith(ORG_ECLIPSE_EQUINOX_LAUNCHER + '.'))
				key = ORG_ECLIPSE_EQUINOX_LAUNCHER;
			else
				key = bundle.getSymbolicName();
			if (result.configurationIUs.containsKey(key)) {
				((Set) result.configurationIUs.get(key)).add(cu);
			} else {
				Set set = new HashSet();
				set.add(cu);
				result.configurationIUs.put(key, set);
			}
		}

		IMetadataRepository metadataRepository = info.getMetadataRepository();
		if (metadataRepository != null && !defaults.isEmpty()) {
			// Product Query will run against the repo later in createProductIU, make sure these CUs are in before then
			metadataRepository.addInstallableUnits(defaults);
		}
	}

	/**
	 * Short term fix to ensure IUs that have no corresponding category are not lost.
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=211521.
	 * @deprecated moved to RootIUAction
	 */
	private IInstallableUnit generateDefaultCategory(IInstallableUnit rootIU) {
		rootCategory.add(rootIU);

		InstallableUnitDescription cat = new MetadataFactory.InstallableUnitDescription();
		cat.setSingleton(true);
		String categoryId = rootIU.getId() + ".categoryIU"; //$NON-NLS-1$
		cat.setId(categoryId);
		cat.setVersion(Version.emptyVersion);
		cat.setProperty(IInstallableUnit.PROP_NAME, rootIU.getProperty(IInstallableUnit.PROP_NAME));
		cat.setProperty(IInstallableUnit.PROP_DESCRIPTION, rootIU.getProperty(IInstallableUnit.PROP_DESCRIPTION));

		ArrayList required = new ArrayList(rootCategory.size());
		for (Iterator iterator = rootCategory.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			required.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), VersionRange.emptyRange, iu.getFilter(), false, false));
		}
		cat.setRequiredCapabilities((IRequirement[]) required.toArray(new IRequirement[required.size()]));
		cat.setCapabilities(new IProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, categoryId, Version.emptyVersion)});
		cat.setArtifacts(new IArtifactKey[0]);
		cat.setProperty(InstallableUnitDescription.PROP_TYPE_CATEGORY, "true"); //$NON-NLS-1$
		return MetadataFactory.createInstallableUnit(cat);
	}

	/** 
	 * @deprecated moved to DefaultCUsAction
	 */
	private void generateDefaultConfigIU(Set ius) {
		//		TODO this is a bit of a hack.  We need to have the default IU fragment generated with code that configures
		//		and unconfigures.  The Generator should be decoupled from any particular provider but it is not clear
		//		that we should add the create* methods to IGeneratorInfo...
		//		MockBundleDescription bd1 = new MockBundleDescription("defaultConfigure");
		//		MockBundleDescription bd2 = new MockBundleDescription("defaultUnconfigure");
		EclipseInstallGeneratorInfoProvider provider = (EclipseInstallGeneratorInfoProvider) info;
		ius.add(MetadataGeneratorHelper.createDefaultBundleConfigurationUnit(provider.createDefaultConfigurationBundleInfo(), provider.createDefaultUnconfigurationBundleInfo(), info.getFlavor()));
		ius.add(MetadataGeneratorHelper.createDefaultFeatureConfigurationUnit(info.getFlavor()));
		ius.add(MetadataGeneratorHelper.createDefaultConfigurationUnitForSourceBundles(info.getFlavor()));
	}

	/**
	 * This method generates IUs for the launchers found in the org.eclipse.executable feature, if present.
	 * @return <code>true</code> if the executable feature was processed successfully,
	 * and <code>false</code> otherwise.
	 * @deprecated moved to ExecutablesDescriptor and EquinoxExecutableAction
	 */
	private boolean generateExecutableFeatureIUs(GeneratorResult result, IArtifactRepository destination) {
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
					generateExecutableIUs(ws, os, arch, versionString, archDirs[archIndex], result, destination);
				}
			}
		}
		return true;
	}

	/**
	 * Generates IUs and CUs for the files that make up the launcher for a given
	 * ws/os/arch combination.
	 * @deprecated moved to EquinoxExecutableAction
	 */
	private void generateExecutableIUs(String ws, String os, final String arch, String version, File root, GeneratorResult result, IArtifactRepository destination) {
		if (root == null)
			return;

		//Create the IU
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(true);
		String productNamespace = (productFile != null) ? productFile.getId() : "org.eclipse"; //$NON-NLS-1$
		String launcherIdPrefix = productNamespace + PRODUCT_LAUCHER_SUFFIX;
		String launcherId = launcherIdPrefix + '.' + ws + '.' + os + '.' + arch;
		iu.setId(launcherId);
		Version launcherVersion = Version.create(version);
		iu.setVersion(launcherVersion);
		iu.setSingleton(true);
		Filter filter = null;
		if (!ws.equals(CONFIG_ANY) && !os.equals(CONFIG_ANY) && !arch.equals(CONFIG_ANY)) {
			filter = ExpressionUtil.parseLDAP("(& (osgi.ws=" + ws + ") (osgi.os=" + os + ") (osgi.arch=" + arch + "))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			iu.setFilter(filter);
		}

		IArtifactKey key = MetadataGeneratorHelper.createLauncherArtifactKey(launcherId, launcherVersion);
		iu.setArtifacts(new IArtifactKey[] {key});
		iu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_NATIVE);
		IProvidedCapability launcherCapability = MetadataFactory.createProvidedCapability(info.getFlavor() + productNamespace, launcherIdPrefix, launcherVersion);
		iu.setCapabilities(new IProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(launcherId, launcherVersion), launcherCapability});

		String launcherFragment = ORG_ECLIPSE_EQUINOX_LAUNCHER + '.' + ws + '.' + os;
		if (!(Constants.OS_MACOSX.equals(os) && !Constants.ARCH_X86_64.equals(arch)))
			launcherFragment += '.' + arch;
		iu.setRequiredCapabilities(new IRequirement[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, launcherFragment, VersionRange.emptyRange, filter, false, false)});
		result.rootIUs.add(MetadataFactory.createInstallableUnit(iu));

		//Create the CU
		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = info.getFlavor() + launcherId;
		cu.setId(configUnitId);
		cu.setVersion(launcherVersion);
		if (filter != null)
			cu.setFilter(filter);
		cu.setHost(new IRequirement[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, launcherId, new VersionRange(launcherVersion, true, launcherVersion, true), null, false, false)});
		cu.setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		//TODO bug 218890, would like the fragment to provide the launcher capability as well, but can't right now.
		cu.setCapabilities(new IProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(configUnitId, launcherVersion)});

		mungeLauncherFileNames(root);

		cu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_NATIVE);
		Map touchpointData = new HashMap();
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$

		IInstallableUnit launcherNameIU = null;

		File executableLocation = info.getExecutableLocation();
		if (executableLocation != null) {
			if (!executableLocation.exists()) {
				if (Constants.OS_WIN32.equals(os) && !executableLocation.getName().endsWith(".exe")) { //$NON-NLS-1$
					executableLocation = new File(executableLocation.getParentFile(), executableLocation.getName() + ".exe"); //$NON-NLS-1$
				} else if (Constants.OS_MACOSX.equals(os)) {
					String name = executableLocation.getName();
					File parent = executableLocation.getParentFile();
					executableLocation = new File(parent, name + ".app/Contents/MacOS/" + name); //$NON-NLS-1$
				}
			}

			if (executableLocation.exists() && executableLocation.isFile())
				launcherNameIU = MetadataGeneratorHelper.generateLauncherSetter(executableLocation.getName(), launcherId, launcherVersion, os, ws, arch, result.rootIUs);
		}

		if (launcherNameIU == null && productFile != null && productFile.getLauncherName() != null) {
			launcherNameIU = MetadataGeneratorHelper.generateLauncherSetter(productFile.getLauncherName(), launcherId, launcherVersion, os, ws, arch, result.rootIUs);
		}

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
						if (launcherNameIU == null && launcherFiles[i].isFile() && new Path(launcherFiles[j].getName()).getFileExtension() == null)
							launcherNameIU = MetadataGeneratorHelper.generateLauncherSetter(launcherFiles[j].getName(), launcherId, launcherVersion, os, ws, arch, result.rootIUs);
					}
				}
			}
		} else if (!Constants.OS_WIN32.equals(os)) {
			File[] launcherFiles = root.listFiles();
			for (int i = 0; launcherFiles != null && i < launcherFiles.length; i++) {
				configurationData += " chmod(targetDir:${installFolder}, targetFile:" + launcherFiles[i].getName() + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$
				if (launcherNameIU == null && launcherFiles[i].isFile() && new Path(launcherFiles[i].getName()).getFileExtension() == null)
					launcherNameIU = MetadataGeneratorHelper.generateLauncherSetter(launcherFiles[i].getName(), launcherId, launcherVersion, os, ws, arch, result.rootIUs);
			}
		} else if (launcherNameIU == null) {
			//windows
			File[] launcherFiles = root.listFiles(new FilenameFilter() {
				public boolean accept(File parent, String name) {
					return name.endsWith(".exe"); //$NON-NLS-1$
				}
			});
			if (launcherFiles != null && launcherFiles.length > 0)
				launcherNameIU = MetadataGeneratorHelper.generateLauncherSetter(launcherFiles[0].getName(), launcherId, launcherVersion, os, ws, arch, result.rootIUs);
		}
		touchpointData.put("install", configurationData); //$NON-NLS-1$
		String unConfigurationData = "cleanupzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("uninstall", unConfigurationData); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		IInstallableUnit unit = MetadataFactory.createInstallableUnit(cu);
		result.rootIUs.add(unit);
		//The Product Query will need to include the launcher CU fragments as a workaround to bug 218890
		if (result.configurationIUs.containsKey(launcherIdPrefix)) {
			((Set) result.configurationIUs.get(launcherIdPrefix)).add(unit);
			if (launcherNameIU != null)
				((Set) result.configurationIUs.get(launcherIdPrefix)).add(launcherNameIU);
		} else {
			Set set = new HashSet();
			set.add(unit);
			if (launcherNameIU != null)
				set.add(launcherNameIU);
			result.configurationIUs.put(launcherIdPrefix, set);
		}

		//Create the artifact descriptor
		IArtifactDescriptor descriptor = MetadataGeneratorHelper.createArtifactDescriptor(key, root, false, true);
		publishArtifact(descriptor, root.listFiles(), destination, false, root);
	}

	/**
	 * For each platform, generate a CU containing the information for the config.ini
	 * @deprecated moved to ProductAction and ConfigCUsAction
	 */
	private void generateProductConfigCUs(GeneratorResult result) {
		for (Iterator iterator = result.configData.keySet().iterator(); iterator.hasNext();) {
			String launcherConfig = (String) iterator.next();
			String[] config = parseConfigSpec(launcherConfig);
			String ws = config[1];
			String os = config[0];
			String arch = config[2];

			ConfigData data = (ConfigData) result.configData.get(launcherConfig);

			InstallableUnitDescription cu = new MetadataFactory.InstallableUnitDescription();
			String configUnitId = info.getFlavor() + productFile.getId() + ".config." + ws + '.' + os + '.' + arch; //$NON-NLS-1$

			String version = getProductVersion();
			Version cuVersion = Version.create(version);
			cu.setId(configUnitId);
			cu.setVersion(cuVersion);
			cu.setSingleton(true);
			cu.setFilter("(& (osgi.ws=" + ws + ") (osgi.os=" + os + ") (osgi.arch=" + arch + "))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			IProvidedCapability productConfigCapability = MetadataFactory.createProvidedCapability(info.getFlavor() + productFile.getId(), productFile.getId() + PRODUCT_CONFIG_SUFFIX, cuVersion);
			IProvidedCapability selfCapability = MetadataGeneratorHelper.createSelfCapability(configUnitId, cuVersion);
			cu.setCapabilities(new IProvidedCapability[] {selfCapability, productConfigCapability});

			cu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_OSGI);
			Map touchpointData = new HashMap();
			String[] dataStrings = getConfigurationStrings(data);
			touchpointData.put("configure", dataStrings[0]); //$NON-NLS-1$
			touchpointData.put("unconfigure", dataStrings[1]); //$NON-NLS-1$
			cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

			result.rootIUs.add(MetadataFactory.createInstallableUnit(cu));
		}
	}

	/**
	 * For the given platform (ws, os, arch) generate the CU that will populate the product.ini file 
	 * @deprecated moved to ProductAction and ConfigCUsAction
	 */
	private void generateProductIniCU(String ws, String os, String arch, String version, GeneratorResult result) {
		if (productFile == null)
			return;

		//attempt to merge arguments from the launcher data and the product file
		Set jvmArgs = new LinkedHashSet();
		Set progArgs = new LinkedHashSet();
		LauncherData launcherData = info.getLauncherData();
		if (launcherData != null) {
			jvmArgs.addAll(Arrays.asList(launcherData.getJvmArgs()));
			progArgs.addAll(Arrays.asList(launcherData.getProgramArgs()));
		}
		progArgs.addAll(Arrays.asList(getArrayFromString(productFile.getProgramArguments(os), " "))); //$NON-NLS-1$
		jvmArgs.addAll(Arrays.asList(getArrayFromString(productFile.getVMArguments(os), " "))); //$NON-NLS-1$

		String[] dataStrings = getLauncherConfigStrings((String[]) jvmArgs.toArray(new String[jvmArgs.size()]), (String[]) progArgs.toArray(new String[progArgs.size()]));
		String configurationData = dataStrings[0];
		String unconfigurationData = dataStrings[1];

		InstallableUnitDescription cu = new MetadataFactory.InstallableUnitDescription();
		String configUnitId = info.getFlavor() + productFile.getId() + ".ini." + ws + '.' + os + '.' + arch; //$NON-NLS-1$
		Version cuVersion = Version.create(version);
		cu.setId(configUnitId);
		cu.setVersion(cuVersion);
		cu.setSingleton(true);
		cu.setFilter("(& (osgi.ws=" + ws + ") (osgi.os=" + os + ") (osgi.arch=" + arch + "))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		IProvidedCapability productIniCapability = MetadataFactory.createProvidedCapability(info.getFlavor() + productFile.getId(), productFile.getId() + PRODUCT_INI_SUFFIX, cuVersion);
		IProvidedCapability selfCapability = MetadataGeneratorHelper.createSelfCapability(configUnitId, cuVersion);
		cu.setCapabilities(new IProvidedCapability[] {selfCapability, productIniCapability});

		cu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_OSGI);
		Map touchpointData = new HashMap();
		touchpointData.put("configure", configurationData); //$NON-NLS-1$
		touchpointData.put("unconfigure", unconfigurationData); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		result.rootIUs.add(MetadataFactory.createInstallableUnit(cu));

	}

	/**
	 * Generates metadata for the given features.
	 * @deprecated moved to FeaturesAction
	 */
	protected void generateFeatureIUs(Feature[] features, GeneratorResult result, IArtifactRepository destination) {
		Map categoriesToFeatureIUs = new HashMap();
		Map featuresToCategories = getFeatureToCategoryMappings();
		//Build Feature IUs, and add them to any corresponding categories
		for (int i = 0; i < features.length; i++) {
			Feature feature = features[i];
			//publish feature site references
			URLEntry updateURL = feature.getUpdateSite();
			//don't enable feature update sites by default since this results in too many
			//extra sites being loaded and searched (Bug 234177)
			if (updateURL != null)
				generateSiteReference(updateURL.getURL(), updateURL.getAnnotation(), feature.getId(), false);
			URLEntry[] discoverySites = feature.getDiscoverySites();
			for (int j = 0; j < discoverySites.length; j++)
				generateSiteReference(discoverySites[j].getURL(), discoverySites[j].getAnnotation(), feature.getId(), false);

			//generate feature IU
			String location = feature.getLocation();
			boolean isExploded = (location.endsWith(".jar") ? false : true); //$NON-NLS-1$
			IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureJarIU(feature, true);
			Collection artifacts = featureIU.getArtifacts();
			storePluginShape(feature, result);
			for (Iterator iterator = artifacts.iterator(); iterator.hasNext();) {
				IArtifactDescriptor ad = MetadataGeneratorHelper.createArtifactDescriptor((IArtifactKey) iterator.next(), new File(location), true, false);
				if (isExploded)
					publishArtifact(ad, new File(location).listFiles(), destination, false, new File(location));
				else
					publishArtifact(ad, new File[] {new File(location)}, destination, true);
			}
			IInstallableUnit generated = MetadataGeneratorHelper.createGroupIU(feature, featureIU);
			result.rootIUs.add(generated);
			result.rootIUs.add(featureIU);

			// @deprecated  moved to SiteXMLAction
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

	/**
	 * @deprecated moved to FeaturesAction
	 */
	private void storePluginShape(Feature feature, GeneratorResult result) {
		FeatureEntry[] entries = feature.getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isPlugin() || entries[i].isFragment()) {
				result.getPluginShapeInfo().put(entries[i].getId() + '_' + entries[i].getVersion(), entries[i].isUnpack() ? BundleDescriptionFactory.DIR : BundleDescriptionFactory.JAR);
			}
		}
	}

	/**
	 * @deprecated moved to various other places.  mainly the aggregator actions (e.g., EclipseInstallAction)
	 */
	protected void generateNativeIUs(File executableLocation, GeneratorResult result, IArtifactRepository destination) {
		//generate data for JRE
		File jreLocation = info.getJRELocation();
		IArtifactDescriptor artifact = MetadataGeneratorHelper.createJREData(jreLocation, result.rootIUs);
		publishArtifact(artifact, new File[] {jreLocation}, destination, false);

		if (info.getLauncherConfig() != null) {
			String[] config = parseConfigSpec(info.getLauncherConfig());
			String version = getProductVersion();
			File root = null;
			if (executableLocation != null)
				root = executableLocation.getParentFile();
			else if (info instanceof EclipseInstallGeneratorInfoProvider)
				root = ((EclipseInstallGeneratorInfoProvider) info).getBaseLocation();
			generateExecutableIUs(config[1], config[0], config[2], version, root, result, destination);
			generateProductIniCU(config[1], config[0], config[2], version, result);
			return;
		}

		//If the executable feature is present, use it to generate IUs for launchers
		if (generateExecutableFeatureIUs(result, destination) || executableLocation == null)
			return;

		//generate data for executable launcher
		artifact = MetadataGeneratorHelper.createLauncherIU(executableLocation, info.getFlavor(), result.rootIUs);
		File[] launcherFiles = null;
		//hard-coded name is ok, since console launcher is not branded, and appears on Windows only
		File consoleLauncher = new File(executableLocation.getParentFile(), "eclipsec.exe"); //$NON-NLS-1$
		if (consoleLauncher.exists())
			launcherFiles = new File[] {executableLocation, consoleLauncher};
		else
			launcherFiles = new File[] {executableLocation};
		publishArtifact(artifact, launcherFiles, destination, false);
	}

	/**
	 * @deprecated moved to various other places.  mainly the aggregator actions (e.g., EclipseInstallAction)
	 */
	protected void generateRootIU(GeneratorResult result, String rootIUId, String rootIUVersion) {
		IInstallableUnit rootIU = null;

		if (info.getProductFile() != null)
			rootIU = createProductIU(result);
		else if (rootIUId != null)
			rootIU = createTopLevelIU(result, rootIUId, rootIUVersion);

		if (rootIU == null)
			return;

		result.nonRootIUs.add(rootIU);
		result.nonRootIUs.add(generateDefaultCategory(rootIU));
	}

	/**
	 * Generates and publishes a reference to an update site location
	 * @param location The update site location
	 * @param featureId the identifier of the feature where the error occurred, or null
	 * @param isEnabled Whether the site should be enabled by default
	 * @deprecated moved to FeaturesAction
	 */
	private void generateSiteReference(String location, String name, String featureId, boolean isEnabled) {
		IMetadataRepository metadataRepo = info.getMetadataRepository();
		try {
			URI associateLocation = URIUtil.fromString(location);
			int flags = isEnabled ? IRepository.ENABLED : IRepository.NONE;
			ArrayList refs = new ArrayList();
			refs.add(new RepositoryReference(associateLocation, name, IRepository.TYPE_METADATA, flags));
			refs.add(new RepositoryReference(associateLocation, name, IRepository.TYPE_ARTIFACT, flags));
			metadataRepo.addReferences(refs);
		} catch (URISyntaxException e) {
			String message = "Invalid site reference: " + location; //$NON-NLS-1$
			if (featureId != null)
				message = message + " in feature: " + featureId; //$NON-NLS-1$
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message));
		}
	}

	/**
	 * @deprecated moved to BundlesAction
	 */
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
			BundleDescription desc = factory.getBundleDescription(bundleLocations[i]);
			if (desc != null)
				result[i] = desc;
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

	/**
	 * @deprecated moved to BundlesAction
	 */
	protected BundleDescriptionFactory getBundleFactory() {
		return new BundleDescriptionFactory(stateObjectFactory, null);
	}

	/**
	 * Returns the categories corresponding to the given feature, or null if there
	 * are no applicable categories.
	 * @param feature The feature to return categories for
	 * @param featuresToCategories A map of SiteFeature->Set<SiteCategory>
	 * @return A Set<SiteCategory> of the categories corresponding to the feature, or <code>null</code>
	 * @deprecated moved to SiteXMLAction
	 */
	private Set getCategories(Feature feature, Map featuresToCategories) {
		//find the SiteFeature corresponding to the given feature
		for (Iterator it = featuresToCategories.keySet().iterator(); it.hasNext();) {
			SiteFeature siteFeature = (SiteFeature) it.next();
			String siteVersion = siteFeature.getFeatureVersion();
			if (!siteFeature.getFeatureIdentifier().equals(feature.getId()))
				continue;
			if (siteVersion.endsWith("qualifier")) { //$NON-NLS-1$
				String withoutQualifier = siteVersion.substring(0, siteVersion.lastIndexOf("qualifier")); //$NON-NLS-1$
				String featureVersion = feature.getVersion();
				if (featureVersion.length() >= withoutQualifier.length() && featureVersion.substring(0, withoutQualifier.length()).equals(withoutQualifier))
					return (Set) featuresToCategories.get(siteFeature);
			} else if (siteFeature.getFeatureVersion().equals(feature.getVersion())) {
				return (Set) featuresToCategories.get(siteFeature);
			}
		}
		return null;
	}

	/**
	 * @deprecated moved to FeaturesAction
	 */
	protected Feature[] getFeatures(File folder) {
		if (folder == null || !folder.exists())
			return new Feature[0];
		File[] locations = folder.listFiles();
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

	/**
	 * Computes the mapping of features to categories as defined in the site.xml,
	 * if available. Returns an empty map if there is not site.xml, or no categories.
	 * @return A map of SiteFeature -> Set<SiteCategory>.
	 */
	protected Map getFeatureToCategoryMappings() {
		HashMap mappings = new HashMap();
		URI siteLocation = info.getSiteLocation();
		if (siteLocation == null)
			return mappings;
		InputStream input;
		SiteModel site = null;
		try {
			input = new BufferedInputStream(URIUtil.toURL(siteLocation).openStream());
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
				mirrors = mirrors.substring(0, index) + mirrors.substring(index + "site.xml".length()); //$NON-NLS-1$
			info.getMetadataRepository().setProperty(IRepository.PROP_MIRRORS_URL, mirrors);
			info.getArtifactRepository().setProperty(IRepository.PROP_MIRRORS_URL, mirrors);
		}

		//publish associate sites as repository references
		URLEntry[] associatedSites = site.getAssociatedSites();
		if (associatedSites != null)
			for (int i = 0; i < associatedSites.length; i++)
				generateSiteReference(associatedSites[i].getURL(), associatedSites[i].getAnnotation(), null, true);

		if (PROTOCOL_FILE.equals(siteLocation.getScheme())) {
			File siteFile = URIUtil.toFile(siteLocation);
			if (siteFile.exists()) {
				File siteParent = siteFile.getParentFile();

				List messageKeys = site.getMessageKeys();
				if (siteParent.isDirectory()) {
					String[] keyStrings = (String[]) messageKeys.toArray(new String[messageKeys.size()]);
					site.setLocalizations(LocalizationHelper.getDirPropertyLocalizations(siteParent, "site", null, keyStrings)); //$NON-NLS-1$
				} else if (siteFile.getName().endsWith(".jar")) { //$NON-NLS-1$
					String[] keyStrings = (String[]) messageKeys.toArray(new String[messageKeys.size()]);
					site.setLocalizations(LocalizationHelper.getJarPropertyLocalizations(siteParent, "site", null, keyStrings)); //$NON-NLS-1$
				}
			}
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

	/**
	 * @TODO This method is a temporary hack to rename the launcher.exe files
	 * to eclipse.exe (or "launcher" to "eclipse"). Eventually we will either hand-craft
	 * metadata/artifacts for launchers, or alter the delta pack to contain eclipse-branded
	 * launchers.
	 * @deprecated moved to EquinoxExecutableAction
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

	protected void publishArtifact(IArtifactDescriptor descriptor, File[] files, IArtifactRepository destination, boolean asIs) {
		publishArtifact(descriptor, files, destination, asIs, null);
	}

	// Put the artifact on the server
	/**
	 * @deprecated moved to AbstractPublishingAction
	 */
	protected void publishArtifact(IArtifactDescriptor descriptor, File[] files, IArtifactRepository destination, boolean asIs, File root) {
		if (descriptor == null || destination == null)
			return;
		if (!info.publishArtifacts()) {
			destination.addDescriptor(descriptor);
			return;
		}
		if (asIs && files.length == 1) {
			try {
				if (!destination.contains(descriptor)) {
					if (destination instanceof IFileArtifactRepository) {
						//if the file is already in the same location the repo will put it, just add the descriptor and exit
						File descriptorFile = ((IFileArtifactRepository) destination).getArtifactFile(descriptor);
						if (files[0].equals(descriptorFile)) {
							destination.addDescriptor(descriptor);
							return;
						}
					}
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
				IPathComputer computer = null;
				if (root != null)
					computer = FileUtils.createRootPathComputer(root);
				else
					computer = FileUtils.createDynamicPathComputer(1);
				FileUtils.zip(files, null, tempFile, computer);
				if (!destination.contains(descriptor)) {
					destination.setProperty(IArtifactDescriptor.DOWNLOAD_CONTENTTYPE, IArtifactDescriptor.TYPE_ZIP);
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
