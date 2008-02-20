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
		/**
		 * The set of generated IUs that will be children of the root IU
		 */
		final Set rootIUs = new HashSet();
		/**
		 * The set of generated IUs that will not be children of the root IU
		 */
		final Set nonRootIUs = new HashSet();

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

	protected IInstallableUnit createProductIU() {
		GeneratorResult result = new GeneratorResult();

		ProductQuery query = new ProductQuery(productFile, info.getFlavor());
		Collector collector = info.getMetadataRepository().query(query, new Collector(), null);
		for (Iterator iterator = collector.iterator(); iterator.hasNext();) {
			result.rootIUs.add(iterator.next());
		}

		//TODO get a real version
		String version = info.getRootVersion() != null ? info.getRootVersion() : "0.0.0"; //$NON-NLS-1$
		ArrayList requires = new ArrayList(1);
		requires.add(MetadataFactory.createRequiredCapability(productFile.getId(), productFile.getId() + ".launcher", VersionRange.emptyRange, null, false, true)); //$NON-NLS-1$
		requires.add(MetadataFactory.createRequiredCapability(productFile.getId(), productFile.getId() + ".ini", VersionRange.emptyRange, null, true, false)); //$NON-NLS-1$
		InstallableUnitDescription root = createTopLevelIUDescription(result, productFile.getId(), version, productFile.getProductName(), requires, false);
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
		root.setApplicabilityFilter(""); //$NON-NLS-1$
		root.setArtifacts(new IArtifactKey[0]);

		root.setProperty("lineUp", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		root.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(configurationIdentification, VersionRange.emptyRange, IUpdateDescriptor.NORMAL, null));
		ProvidedCapability groupCapability = MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_KIND, "group", new Version("1.0.0")); //$NON-NLS-1$ //$NON-NLS-2$
		root.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(configurationIdentification, new Version(configurationVersion)), groupCapability});
		root.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_ECLIPSE);
		Map touchpointData = new HashMap();

		String configurationData = ""; //$NON-NLS-1$
		String unconfigurationData = ""; //$NON-NLS-1$

		ConfigData configData = info.getConfigData();
		if (configData != null) {
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
		}

		if (configureLauncherData) {
			LauncherData launcherData = info.getLauncherData();
			if (launcherData != null) {
				final String[] jvmArgs = launcherData.getJvmArgs();
				for (int i = 0; i < jvmArgs.length; i++) {
					configurationData += "addJvmArg(jvmArg:" + jvmArgs[i] + ");"; //$NON-NLS-1$ //$NON-NLS-2$
					unconfigurationData += "removeJvmArg(jvmArg:" + jvmArgs[i] + ");"; //$NON-NLS-1$ //$NON-NLS-2$
				}

				final String[] programArgs = launcherData.getProgramArgs();
				for (int i = 0; i < programArgs.length; i++) {
					String programArg = programArgs[i];
					if (programArg.equals("--launcher.library") || programArg.equals("-startup") || programArg.equals("-configuration")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						i++;
					configurationData += "addProgramArg(programArg:" + programArg + ");"; //$NON-NLS-1$ //$NON-NLS-2$
					unconfigurationData += "removeProgramArg(programArg:" + programArg + ");"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		touchpointData.put("configure", configurationData); //$NON-NLS-1$
		touchpointData.put("unconfigure", unconfigurationData); //$NON-NLS-1$
		root.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		return root;
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

		generateConfigIUs(info.getConfigData() == null ? null : info.getConfigData().getBundles(), result);

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

	protected void generateConfigIUs(BundleInfo[] infos, GeneratorResult result) {
		if (infos != null) {
			for (int i = 0; i < infos.length; i++) {
				GeneratorBundleInfo bundle = new GeneratorBundleInfo(infos[i]);
				if (bundle.getSymbolicName().equals(ORG_ECLIPSE_UPDATE_CONFIGURATOR)) {
					bundle.setStartLevel(BundleInfo.NO_LEVEL);
					bundle.setMarkedAsStarted(false);
					bundle.setSpecialConfigCommands("addJvmArg(jvmArg:-Dorg.eclipse.update.reconcile=false);" + //$NON-NLS-1$
							"addJvmArg(jvmArg:-Dorg.eclipse.p2.update.compatibility=false);"); //$NON-NLS-1$
					bundle.setSpecialUnconfigCommands("removeJvmArg(jvmArg:-Dorg.eclipse.update.reconcile=false);" + //$NON-NLS-1$
							"removeJvmArg(jvmArg:-Dorg.eclipse.p2.update.compatibility=false);"); //$NON-NLS-1$
				}
				if (bundle.getSymbolicName().equals(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR)) {
					bundle.setSpecialConfigCommands("addJvmArg(jvmArg:-Dorg.eclipse.equinox.simpleconfigurator.useReference=true);"); //$NON-NLS-1$
					bundle.setSpecialUnconfigCommands("removeJvmArg(jvmArg:-Dorg.eclipse.equinox.simpleconfigurator.useReference=true);"); //$NON-NLS-1$
				}
				IInstallableUnit cu = MetadataGeneratorHelper.createBundleConfigurationUnit(bundle.getSymbolicName(), new Version(bundle.getVersion()), false, bundle, info.getFlavor(), null);
				if (cu != null)
					result.rootIUs.add(cu);
			}
		}

		if (info.addDefaultIUs()) {
			for (Iterator iterator = info.getDefaultIUs(result.rootIUs).iterator(); iterator.hasNext();) {
				GeneratorBundleInfo bundle = (GeneratorBundleInfo) iterator.next();
				IInstallableUnit configuredIU = result.getInstallableUnit(bundle.getSymbolicName());
				if (configuredIU != null)
					bundle.setVersion(configuredIU.getVersion().toString());
				String filter = configuredIU == null ? null : configuredIU.getFilter();
				IInstallableUnit cu = MetadataGeneratorHelper.createBundleConfigurationUnit(bundle.getSymbolicName(), new Version(bundle.getVersion()), false, bundle, info.getFlavor(), filter);
				//the configuration unit should share the same platform filter as the IU being configured.
				if (cu != null)
					result.rootIUs.add(cu);
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
		cat.setApplicabilityFilter(""); //$NON-NLS-1$
		cat.setArtifacts(new IArtifactKey[0]);
		cat.setProperty(IInstallableUnit.PROP_CATEGORY_IU, "true"); //$NON-NLS-1$
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
		ProvidedCapability launcherCapability = MetadataFactory.createProvidedCapability(productNamespace, launcherIdPrefix, new Version("1.0.0")); //$NON-NLS-1$
		iu.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(launcherId, launcherVersion), launcherCapability});

		String launcherFragment = "org.eclipse.equinox.launcher." + ws + '.' + os; //$NON-NLS-1$
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
		//TODO bug 218890, would like the fragment to provide the launcher capability as well, but can't right now.
		cu.setCapabilities(new ProvidedCapability[] {IInstallableUnitFragment.FRAGMENT_CAPABILITY, MetadataGeneratorHelper.createSelfCapability(configUnitId, launcherVersion)});

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
		result.rootIUs.add(MetadataFactory.createInstallableUnit(cu));

		//Create the artifact descriptor
		IArtifactDescriptor descriptor = MetadataGeneratorHelper.createArtifactDescriptor(key, root, false, true);
		publishArtifact(descriptor, root.listFiles(), destination, false);
	}

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

		String configurationData = ""; //$NON-NLS-1$
		String unconfigurationData = ""; //$NON-NLS-1$
		for (Iterator iterator = jvmArgs.iterator(); iterator.hasNext();) {
			String arg = (String) iterator.next();
			configurationData += "addJvmArg(jvmArg:" + arg + ");"; //$NON-NLS-1$ //$NON-NLS-2$
			unconfigurationData += "removeJvmArg(jvmArg:" + arg + ");"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		for (Iterator iterator = progArgs.iterator(); iterator.hasNext();) {
			String arg = (String) iterator.next();
			if (arg.equals("--launcher.library") || arg.equals("-startup") || arg.equals("-configuration")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				continue;
			configurationData += "addProgramArg(programArg:" + arg + ");"; //$NON-NLS-1$ //$NON-NLS-2$
			unconfigurationData += "removeProgramArg(programArg:" + arg + ");"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (configurationData.length() == 0)
			return;

		InstallableUnitDescription cu = new MetadataFactory.InstallableUnitDescription();
		String configUnitId = info.getFlavor() + productFile.getId() + ".ini." + os; //$NON-NLS-1$
		Version cuVersion = new Version(version);
		cu.setId(configUnitId);
		cu.setVersion(cuVersion);
		cu.setFilter("(& (osgi.ws=" + ws + ") (osgi.os=" + os + ") (osgi.arch=" + arch + "))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		ProvidedCapability productIniCapability = MetadataFactory.createProvidedCapability(productFile.getId(), productFile.getId() + ".ini", Version.emptyVersion); //$NON-NLS-1$
		ProvidedCapability selfCapability = MetadataGeneratorHelper.createSelfCapability(configUnitId, cuVersion);
		cu.setCapabilities(new ProvidedCapability[] {IInstallableUnitFragment.FRAGMENT_CAPABILITY, selfCapability, productIniCapability});

		cu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_ECLIPSE);
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
			IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureIU(feature, isExploded);
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
			generateExecutableIUs(config[1], config[0], config[2], "1.0.0", executableLocation.getParentFile(), result, destination); //$NON-NLS-1$
			generateProductIniCU(config[1], config[0], config[2], "1.0.0", result); //$NON-NLS-1$
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
			rootIU = createProductIU();
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
