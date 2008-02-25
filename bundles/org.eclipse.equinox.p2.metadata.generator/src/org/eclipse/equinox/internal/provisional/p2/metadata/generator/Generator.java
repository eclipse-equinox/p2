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
	public static class GeneratorResult {
		public static final String CONFIGURATION_CUS = "CONFIGURATION_CUS"; //$NON-NLS-1$

		/**
		 * The set of generated IUs that will be children of the root IU
		 */
		final Set rootIUs = new HashSet();
		/**
		 * The set of generated IUs that will not be children of the root IU
		 */
		final Set nonRootIUs = new HashSet();

		/**
		 * Map of symbolic name to a set of generated CUs for that IU
		 */
		final Map configurationIUs = new HashMap();

		/**
		 * Map launcherConfig to config.ini ConfigData
		 */
		final Map configData = new HashMap();

		/**
		 * Returns all IUs generated during this execution of the generator.
		 */
		Set allGeneratedIUs() {
			HashSet all = new HashSet();
			all.addAll(rootIUs);
			all.addAll(nonRootIUs);
			return all;
		}

		/**
		 * Returns the IU in this result with the given id.
		 */
		IInstallableUnit getInstallableUnit(String id) {
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

	}

	private static final String ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR = "org.eclipse.equinox.simpleconfigurator"; //$NON-NLS-1$
	private static final String ORG_ECLIPSE_UPDATE_CONFIGURATOR = "org.eclipse.update.configurator"; //$NON-NLS-1$
	private static final String ORG_ECLIPSE_EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher"; //$NON-NLS-1$

	//	private static String[][] defaultMappingRules = new String[][] { {"(& (namespace=eclipse) (classifier=feature))", "${repoUrl}/feature/${id}_${version}"}, {"(& (namespace=eclipse) (classifier=plugin))", "${repoUrl}/plugin/${id}_${version}"}, {"(& (namespace=eclipse) (classifier=native))", "${repoUrl}/native/${id}_${version}"}};

	private final IGeneratorInfo info;

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

	private boolean checkOptionalRootDependency(IInstallableUnit iu) {
		// TODO: This is a kludge to make the default configuration unit
		//		 for features be optional in the root. Since there is a global
		//		 filter to prevent features from being installed, the fragment
		//		 needs to be optional.
		return (iu.getId().indexOf(".feature.default") > 0 ? true : false); //$NON-NLS-1$
	}

	protected IInstallableUnit createProductIU(GeneratorResult result) {
		generateProductConfigCUs(result);

		GeneratorResult productContents = new GeneratorResult();

		ProductQuery query = new ProductQuery(productFile, info.getFlavor(), result.configurationIUs);
		Collector collector = info.getMetadataRepository().query(query, new Collector(), null);
		for (Iterator iterator = collector.iterator(); iterator.hasNext();) {
			productContents.rootIUs.add(iterator.next());
		}

		String version = productFile.getVersion();
		if (version.equals("0.0.0") && info.getRootVersion() != null) //$NON-NLS-1$
			version = info.getRootVersion();
		ArrayList requires = new ArrayList(1);
		requires.add(MetadataFactory.createRequiredCapability(info.getFlavor() + productFile.getId(), productFile.getId() + ".launcher", VersionRange.emptyRange, null, false, true)); //$NON-NLS-1$
		requires.add(MetadataFactory.createRequiredCapability(info.getFlavor() + productFile.getId(), productFile.getId() + ".ini", VersionRange.emptyRange, null, false, false)); //$NON-NLS-1$
		requires.add(MetadataFactory.createRequiredCapability(info.getFlavor() + productFile.getId(), productFile.getId() + ".config", VersionRange.emptyRange, null, false, false)); //$NON-NLS-1$

		//default CUs		
		requires.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, info.getFlavor() + ".plugin.default", VersionRange.emptyRange, null, false, false)); //$NON-NLS-1$
		requires.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, info.getFlavor() + ".source.default", VersionRange.emptyRange, null, true, false)); //$NON-NLS-1$
		if (productFile.useFeatures())
			requires.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, info.getFlavor() + ".feature.default", VersionRange.emptyRange, null, true, false)); //$NON-NLS-1$

		InstallableUnitDescription root = createTopLevelIUDescription(productContents, productFile.getId(), version, productFile.getProductName(), requires, false);
		return MetadataFactory.createInstallableUnit(root);
	}

	protected IInstallableUnit createTopLevelIU(GeneratorResult result, String configurationIdentification, String configurationVersion) {
		// TODO, bit of a hack but for now set the name of the IU to the ID.
		InstallableUnitDescription root = createTopLevelIUDescription(result, configurationIdentification, configurationVersion, configurationIdentification, null, true);
		return MetadataFactory.createInstallableUnit(root);
	}

	protected InstallableUnitDescription createTopLevelIUDescription(GeneratorResult result, String configurationIdentification, String configurationVersion, String configurationName, List requires, boolean configureLauncherData) {
		InstallableUnitDescription root = new MetadataFactory.InstallableUnitDescription();
		root.setSingleton(true);
		root.setId(configurationIdentification);
		root.setVersion(new Version(configurationVersion));
		root.setProperty(IInstallableUnit.PROP_NAME, configurationName);

		ArrayList reqsConfigurationUnits = new ArrayList(result.rootIUs.size());
		for (Iterator iterator = result.rootIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
			boolean isOptional = checkOptionalRootDependency(iu);
			reqsConfigurationUnits.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, iu.getFilter(), isOptional, false));
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

	private String[] getConfigurationStrings(ConfigData configData) {
		String configurationData = ""; //$NON-NLS-1$
		String unconfigurationData = ""; //$NON-NLS-1$
		for (Iterator iterator = configData.getFwDependentProps().entrySet().iterator(); iterator.hasNext();) {
			Entry aProperty = (Entry) iterator.next();
			String key = ((String) aProperty.getKey());
			if (key.equals("osgi.frameworkClassPath") || key.equals("osgi.framework") || key.equals("osgi.bundles") || key.equals("eof")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				continue;
			configurationData += "setFwDependentProp(propName:" + key + ", propValue:" + ((String) aProperty.getValue()) + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			unconfigurationData += "setFwDependentProp(propName:" + key + ", propValue:);"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		for (Iterator iterator = configData.getFwIndependentProps().entrySet().iterator(); iterator.hasNext();) {
			Entry aProperty = (Entry) iterator.next();
			String key = ((String) aProperty.getKey());
			if (key.equals("osgi.frameworkClassPath") || key.equals("osgi.framework") || key.equals("osgi.bundles") || key.equals("eof")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				continue;
			configurationData += "setFwIndependentProp(propName:" + key + ", propValue:" + ((String) aProperty.getValue()) + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			unconfigurationData += "setFwIndependentProp(propName:" + key + ", propValue:);"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		return new String[] {configurationData, unconfigurationData};
	}

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
			metadataRepository.addInstallableUnits((IInstallableUnit[]) allGeneratedUnits.toArray(new IInstallableUnit[allGeneratedUnits.size()]));
		}

		return Status.OK_STATUS;
	}

	protected void generateBundleIUs(BundleDescription[] bundles, GeneratorResult result, IArtifactRepository destination) {
		for (int i = 0; i < bundles.length; i++) {
			BundleDescription bd = bundles[i];
			// A bundle may be null if the associated plug-in does not have a manifest file -
			// for example, org.eclipse.jdt.launching.j9
			if (bd != null && bd.getSymbolicName() != null && bd.getVersion() != null) {
				String format = (String) ((Dictionary) bd.getUserObject()).get(BundleDescriptionFactory.BUNDLE_FILE_KEY);
				boolean isDir = format.equals(BundleDescriptionFactory.DIR) ? true : false;
				IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(bd.getSymbolicName(), bd.getVersion().toString());
				IArtifactDescriptor ad = MetadataGeneratorHelper.createArtifactDescriptor(key, new File(bd.getLocation()), true, false);
				if (isDir)
					publishArtifact(ad, new File(bd.getLocation()).listFiles(), destination, false);
				else
					publishArtifact(ad, new File[] {new File(bd.getLocation())}, destination, true);
				if (info.reuseExistingPack200Files() && !info.publishArtifacts()) {
					File packFile = new Path(bd.getLocation()).addFileExtension("pack.gz").toFile(); //$NON-NLS-1$
					if (packFile.exists()) {
						IArtifactDescriptor ad200 = MetadataGeneratorHelper.createPack200ArtifactDescriptor(key, packFile, ad.getProperty(IArtifactDescriptor.ARTIFACT_SIZE));
						publishArtifact(ad200, new File[] {packFile}, destination, true);
					}
				}
				IInstallableUnit iu = MetadataGeneratorHelper.createBundleIU(bd, (Map) bd.getUserObject(), isDir, key);
				result.rootIUs.add(iu);
			}
		}
	}

	/**
	 * Generates IUs corresponding to update site categories.
	 * @param categoriesToFeatures Map of SiteCategory ->Set (Feature IUs in that category).
	 * @param result The generator result being built
	 */
	protected void generateCategoryIUs(Map categoriesToFeatures, GeneratorResult result) {
		for (Iterator it = categoriesToFeatures.keySet().iterator(); it.hasNext();) {
			SiteCategory category = (SiteCategory) it.next();
			result.nonRootIUs.add(MetadataGeneratorHelper.createCategoryIU(category, (Set) categoriesToFeatures.get(category), null));
		}
	}

	private void storeConfigData(GeneratorResult result) {
		if (result.configData.containsKey(info.getLauncherConfig()))
			return; //been here, done this

		File fwConfigFile = new File(info.getLauncherData().getFwConfigLocation(), EquinoxConstants.CONFIG_INI);
		if (fwConfigFile.exists()) {
			if (info instanceof EclipseInstallGeneratorInfoProvider) {
				((EclipseInstallGeneratorInfoProvider) info).loadConfigData(fwConfigFile);
				ConfigData data = info.getConfigData();
				result.configData.put(info.getLauncherConfig(), data);
			}
		}
	}

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

	protected void generateBundleConfigIUs(BundleInfo[] infos, GeneratorResult result, String launcherConfig) {
		if (infos == null)
			return;

		String cuIdPrefix = ""; //$NON-NLS-1$
		String filter = null;
		if (launcherConfig != null) {
			//launcher config is os_ws_arch, we want suffix ws.os.arch
			String[] config = getArrayFromString(launcherConfig, "_"); //$NON-NLS-1$
			cuIdPrefix = config[1] + '.' + config[0] + '.' + config[2];

			filter = "(& (osgi.ws=" + config[1] + ") (osgi.os=" + config[0] + ") (osgi.arch=" + config[2] + "))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		for (int i = 0; i < infos.length; i++) {
			GeneratorBundleInfo bundle = createGeneratorBundleInfo(infos[i], result);
			if (bundle == null)
				continue;

			if (bundle.getSymbolicName().equals(ORG_ECLIPSE_UPDATE_CONFIGURATOR)) {
				bundle.setStartLevel(BundleInfo.NO_LEVEL);
				bundle.setMarkedAsStarted(false);
				bundle.setSpecialConfigCommands("addJvmArg(jvmArg:-Dorg.eclipse.update.reconcile=false);" + //$NON-NLS-1$
						"addJvmArg(jvmArg:-Dorg.eclipse.p2.update.compatibility=false);"); //$NON-NLS-1$
				bundle.setSpecialUnconfigCommands("removeJvmArg(jvmArg:-Dorg.eclipse.update.reconcile=false);" + //$NON-NLS-1$
						"removeJvmArg(jvmArg:-Dorg.eclipse.p2.update.compatibility=false);"); //$NON-NLS-1$
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

	}

	protected void generateConfigIUs(GeneratorResult result) {
		ConfigData data = info.getConfigData();
		if ((data == null || data.getBundles().length == 0) && info.getLauncherConfig() != null) {
			//We have the config.ini but not necessarily all the needed bundle IUs, remember for later
			storeConfigData(result);
		} else if (data != null) {
			// generation against an eclipse install (config.ini + bundles)
			generateBundleConfigIUs(data.getBundles(), result, info.getLauncherConfig());
		} else if (result.configData.size() > 0 && generateRootIU) {
			// generation from remembered config.ini's
			// we have N platforms, generate a CU for each
			// TODO try and find common properties across platforms
			for (Iterator iterator = result.configData.keySet().iterator(); iterator.hasNext();) {
				String launcherConfig = (String) iterator.next();
				data = (ConfigData) result.configData.get(launcherConfig);
				generateBundleConfigIUs(data.getBundles(), result, launcherConfig);
			}
		}

		if (info.addDefaultIUs()) {
			for (Iterator iterator = info.getDefaultIUs(result.rootIUs).iterator(); iterator.hasNext();) {
				GeneratorBundleInfo bundle = (GeneratorBundleInfo) iterator.next();
				IInstallableUnit configuredIU = result.getInstallableUnit(bundle.getSymbolicName());
				if (configuredIU == null)
					continue;
				bundle.setVersion(configuredIU.getVersion().toString());
				String filter = configuredIU == null ? null : configuredIU.getFilter();
				IInstallableUnit cu = MetadataGeneratorHelper.createBundleConfigurationUnit(bundle.getSymbolicName(), new Version(bundle.getVersion()), false, bundle, info.getFlavor(), filter);
				//the configuration unit should share the same platform filter as the IU being configured.
				if (cu != null)
					result.rootIUs.add(cu);
				if (bundle.getSymbolicName().startsWith(ORG_ECLIPSE_EQUINOX_LAUNCHER + '.')) {
					if (result.configurationIUs.containsKey(ORG_ECLIPSE_EQUINOX_LAUNCHER)) {
						((Set) result.configurationIUs.get(ORG_ECLIPSE_EQUINOX_LAUNCHER)).add(cu);
					} else {
						Set set = new HashSet();
						set.add(cu);
						result.configurationIUs.put(ORG_ECLIPSE_EQUINOX_LAUNCHER, set);
					}
				}
			}
		}
	}

	/**
	 * Short term fix to ensure IUs that have no corresponding category are not lost.
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=211521.
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
		cat.setRequiredCapabilities((RequiredCapability[]) required.toArray(new RequiredCapability[required.size()]));
		cat.setCapabilities(new ProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, categoryId, Version.emptyVersion)});
		cat.setArtifacts(new IArtifactKey[0]);
		cat.setProperty(IInstallableUnit.PROP_TYPE_CATEGORY, "true"); //$NON-NLS-1$
		return MetadataFactory.createInstallableUnit(cat);
	}

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
	 */
	private void generateExecutableIUs(String ws, String os, final String arch, String version, File root, GeneratorResult result, IArtifactRepository destination) {
		//Create the IU
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(true);
		String productNamespace = (productFile != null) ? productFile.getId() : "org.eclipse"; //$NON-NLS-1$
		String launcherIdPrefix = productNamespace + ".launcher"; //$NON-NLS-1$
		String launcherId = launcherIdPrefix + '.' + ws + '.' + os + '.' + arch;
		iu.setId(launcherId);
		Version launcherVersion = new Version(version);
		iu.setVersion(launcherVersion);
		String filter = "(& (osgi.ws=" + ws + ") (osgi.os=" + os + ") (osgi.arch=" + arch + "))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		iu.setFilter(filter);

		IArtifactKey key = MetadataGeneratorHelper.createLauncherArtifactKey(launcherId, launcherVersion);
		iu.setArtifacts(new IArtifactKey[] {key});
		iu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_NATIVE);
		ProvidedCapability launcherCapability = MetadataFactory.createProvidedCapability(info.getFlavor() + productNamespace, launcherIdPrefix, new Version("1.0.0")); //$NON-NLS-1$
		iu.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(launcherId, launcherVersion), launcherCapability});

		String launcherFragment = ORG_ECLIPSE_EQUINOX_LAUNCHER + '.' + ws + '.' + os;
		if (!Constants.OS_MACOSX.equals(os))
			launcherFragment += '.' + arch;
		iu.setRequiredCapabilities(new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, launcherFragment, VersionRange.emptyRange, filter, false, false)});
		result.rootIUs.add(MetadataFactory.createInstallableUnit(iu));

		//Create the CU
		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = info.getFlavor() + launcherId;
		cu.setId(configUnitId);
		cu.setVersion(launcherVersion);
		cu.setFilter(filter);
		cu.setHost(launcherId, new VersionRange(launcherVersion, true, launcherVersion, true));
		cu.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		//TODO bug 218890, would like the fragment to provide the launcher capability as well, but can't right now.
		cu.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(configUnitId, launcherVersion)});

		mungeLauncherFileNames(root);

		cu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_NATIVE);
		Map touchpointData = new HashMap();
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		if (Constants.OS_MACOSX.equals(os)) {
			//navigate down to Contents/MacOs
			File[] launcherFiles = root.listFiles()[0].listFiles()[0].listFiles();
			for (int i = 0; i < launcherFiles.length; i++) {
				if (launcherFiles[i].isDirectory()) {
					launcherFiles = launcherFiles[i].listFiles();
				}
				configurationData += " chmod(targetDir:${installFolder}/" + root.listFiles()[0].getName() + "/Contents/MacOS/, targetFile:" + launcherFiles[i].getName() + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				MetadataGeneratorHelper.generateLauncherSetter(new Path(launcherFiles[i].getName()).lastSegment().toString(), launcherId, launcherVersion, os, ws, arch, result.rootIUs);
			}
		}
		if (!Constants.OS_WIN32.equals(os) && !Constants.OS_MACOSX.equals(os)) {
			File[] launcherFiles = root.listFiles();
			for (int i = 0; i < launcherFiles.length; i++) {
				configurationData += " chmod(targetDir:${installFolder}, targetFile:" + launcherFiles[i].getName() + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$
				if (new Path(launcherFiles[i].getName()).getFileExtension() == null)
					MetadataGeneratorHelper.generateLauncherSetter(launcherFiles[i].getName(), launcherId, launcherVersion, os, ws, arch, result.rootIUs);
			}
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
		} else {
			Set set = new HashSet();
			set.add(unit);
			result.configurationIUs.put(launcherIdPrefix, set);
		}

		//Create the artifact descriptor
		IArtifactDescriptor descriptor = MetadataGeneratorHelper.createArtifactDescriptor(key, root, false, true);
		publishArtifact(descriptor, root.listFiles(), destination, false);
	}

	/*
	 * For each platform, generate a CU containing the information for the config.ini
	 */
	private void generateProductConfigCUs(GeneratorResult result) {
		for (Iterator iterator = result.configData.keySet().iterator(); iterator.hasNext();) {
			String launcherConfig = (String) iterator.next();
			String[] config = getArrayFromString(launcherConfig, "_"); //$NON-NLS-1$
			String ws = config[1];
			String os = config[0];
			String arch = config[2];

			ConfigData data = (ConfigData) result.configData.get(launcherConfig);

			InstallableUnitDescription cu = new MetadataFactory.InstallableUnitDescription();
			String configUnitId = info.getFlavor() + productFile.getId() + ".config." + ws + '.' + os + '.' + arch; //$NON-NLS-1$
			Version cuVersion = new Version(productFile.getVersion());
			cu.setId(configUnitId);
			cu.setVersion(cuVersion);
			cu.setFilter("(& (osgi.ws=" + ws + ") (osgi.os=" + os + ") (osgi.arch=" + arch + "))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			ProvidedCapability productConfigCapability = MetadataFactory.createProvidedCapability(info.getFlavor() + productFile.getId(), productFile.getId() + ".config", Version.emptyVersion); //$NON-NLS-1$
			ProvidedCapability selfCapability = MetadataGeneratorHelper.createSelfCapability(configUnitId, cuVersion);
			cu.setCapabilities(new ProvidedCapability[] {selfCapability, productConfigCapability});

			cu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_OSGI);
			Map touchpointData = new HashMap();
			String[] dataStrings = getConfigurationStrings(data);
			touchpointData.put("configure", dataStrings[0]); //$NON-NLS-1$
			touchpointData.put("unconfigure", dataStrings[1]); //$NON-NLS-1$
			cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

			result.rootIUs.add(MetadataFactory.createInstallableUnit(cu));
		}
	}

	/* 
	 * For the given platform (ws, os, arch) generate the CU that will populate the product.ini file 
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

		if (configurationData.length() == 0)
			return;

		InstallableUnitDescription cu = new MetadataFactory.InstallableUnitDescription();
		String configUnitId = info.getFlavor() + productFile.getId() + ".ini." + ws + '.' + os + '.' + arch; //$NON-NLS-1$
		Version cuVersion = new Version(version);
		cu.setId(configUnitId);
		cu.setVersion(cuVersion);
		cu.setFilter("(& (osgi.ws=" + ws + ") (osgi.os=" + os + ") (osgi.arch=" + arch + "))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		ProvidedCapability productIniCapability = MetadataFactory.createProvidedCapability(info.getFlavor() + productFile.getId(), productFile.getId() + ".ini", Version.emptyVersion); //$NON-NLS-1$
		ProvidedCapability selfCapability = MetadataGeneratorHelper.createSelfCapability(configUnitId, cuVersion);
		cu.setCapabilities(new ProvidedCapability[] {selfCapability, productIniCapability});

		cu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_OSGI);
		Map touchpointData = new HashMap();
		touchpointData.put("configure", configurationData); //$NON-NLS-1$
		touchpointData.put("unconfigure", unconfigurationData); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		result.rootIUs.add(MetadataFactory.createInstallableUnit(cu));

	}

	protected void generateFeatureIUs(Feature[] features, GeneratorResult result, IArtifactRepository destination) {
		Map categoriesToFeatureIUs = new HashMap();
		Map featuresToCategories = getFeatureToCategoryMappings();
		//Build Feature IUs, and add them to any corresponding categories
		for (int i = 0; i < features.length; i++) {
			Feature feature = features[i];
			String location = feature.getLocation();
			boolean isExploded = (location.endsWith(".jar") ? false : true); //$NON-NLS-1$
			IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureJarIU(feature, isExploded);
			IArtifactKey[] artifacts = featureIU.getArtifacts();
			for (int arti = 0; arti < artifacts.length; arti++) {
				IArtifactDescriptor ad = MetadataGeneratorHelper.createArtifactDescriptor(artifacts[arti], new File(location), true, false);
				if (isExploded)
					publishArtifact(ad, new File(location).listFiles(), destination, false);
				else
					publishArtifact(ad, new File[] {new File(location)}, destination, true);
			}
			IInstallableUnit generated = MetadataGeneratorHelper.createGroupIU(feature, featureIU);
			result.rootIUs.add(generated);
			result.rootIUs.add(featureIU);
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

	protected void generateNativeIUs(File executableLocation, GeneratorResult result, IArtifactRepository destination) {
		//generate data for JRE
		File jreLocation = info.getJRELocation();
		IArtifactDescriptor artifact = MetadataGeneratorHelper.createJREData(jreLocation, result.rootIUs);
		publishArtifact(artifact, new File[] {jreLocation}, destination, false);

		if (info.getLauncherConfig() != null) {
			String[] config = getArrayFromString(info.getLauncherConfig(), "_"); //$NON-NLS-1$
			String version = "1.0.0"; //$NON-NLS-1$
			if (productFile != null && !productFile.getVersion().equals("0.0.0")) //$NON-NLS-1$
				version = productFile.getVersion();
			generateExecutableIUs(config[1], config[0], config[2], version, executableLocation.getParentFile(), result, destination);
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

	protected BundleDescription[] getBundleDescriptions(File[] bundleLocations) {
		if (bundleLocations == null)
			return new BundleDescription[0];
		boolean addSimpleConfigurator = false;
		for (int i = 0; i < bundleLocations.length; i++) {
			addSimpleConfigurator = bundleLocations[i].toString().indexOf(ORG_ECLIPSE_UPDATE_CONFIGURATOR) > 0;
			if (addSimpleConfigurator)
				break;
		}
		BundleDescription[] result = new BundleDescription[bundleLocations.length + (addSimpleConfigurator ? 1 : 0)];
		BundleDescriptionFactory factory = getBundleFactory();
		for (int i = 0; i < bundleLocations.length; i++)
			result[i] = factory.getBundleDescription(bundleLocations[i]);
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
	private Set getCategories(Feature feature, Map featuresToCategories) {
		//find the SiteFeature corresponding to the given feature
		for (Iterator it = featuresToCategories.keySet().iterator(); it.hasNext();) {
			SiteFeature siteFeature = (SiteFeature) it.next();
			if (siteFeature.getFeatureIdentifier().equals(feature.getId()) && siteFeature.getFeatureVersion().equals(feature.getVersion()))
				return (Set) featuresToCategories.get(siteFeature);
		}
		return null;
	}

	private Feature[] getFeatures(File folder) {
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
		URL siteLocation = info.getSiteLocation();
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
	protected void publishArtifact(IArtifactDescriptor descriptor, File[] files, IArtifactRepository destination, boolean asIs) {
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
				FileUtils.zip(files, tempFile);
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
