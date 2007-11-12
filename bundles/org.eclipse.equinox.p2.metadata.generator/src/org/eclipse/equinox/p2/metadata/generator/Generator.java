/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.metadata.generator;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.generator.Activator;
import org.eclipse.equinox.internal.p2.metadata.generator.features.FeatureParser;
import org.eclipse.equinox.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Version;

public class Generator {

	private static final String ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR = "org.eclipse.equinox.simpleconfigurator";
	private static final String ORG_ECLIPSE_UPDATE_CONFIGURATOR = "org.eclipse.update.configurator";
	//	private static String[][] defaultMappingRules = new String[][] { {"(& (namespace=eclipse) (classifier=feature))", "${repoUrl}/feature/${id}_${version}"}, {"(& (namespace=eclipse) (classifier=plugin))", "${repoUrl}/plugin/${id}_${version}"}, {"(& (namespace=eclipse) (classifier=native))", "${repoUrl}/native/${id}_${version}"}};

	private final IGeneratorInfo info;
	private StateObjectFactory stateObjectFactory;

	public Generator(IGeneratorInfo infoProvider) {
		this.info = infoProvider;
		// TODO need to figure a better way of configuring the generator...
		PlatformAdmin platformAdmin = (PlatformAdmin) ServiceHelper.getService(Activator.getContext(), PlatformAdmin.class.getName());
		if (platformAdmin != null) {
			stateObjectFactory = platformAdmin.getFactory();
		}
	}

	protected IInstallableUnit createTopLevelIU(Set resultantIUs, String configurationIdentification, String configurationVersion) {
		InstallableUnitDescription root = new MetadataFactory.InstallableUnitDescription();
		root.setSingleton(true);
		root.setId(configurationIdentification);
		root.setVersion(new Version(configurationVersion));
		// TODO, bit of a hack but for now set the name of the IU to the ID.
		root.setProperty(IInstallableUnit.PROP_NAME, configurationIdentification);

		ArrayList reqsConfigurationUnits = new ArrayList(resultantIUs.size());
		for (Iterator iterator = resultantIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
			reqsConfigurationUnits.add(new RequiredCapability(IInstallableUnit.NAMESPACE_IU, iu.getId(), range, iu.getFilter(), false, false));
		}
		root.setRequiredCapabilities((RequiredCapability[]) reqsConfigurationUnits.toArray(new RequiredCapability[reqsConfigurationUnits.size()]));
		root.setApplicabilityFilter("");
		root.setArtifacts(new IArtifactKey[0]);

		root.setProperty("lineUp", "true");
		root.setProperty(IInstallableUnit.PROP_UPDATE_FROM, configurationIdentification);
		root.setProperty(IInstallableUnit.PROP_UPDATE_RANGE, VersionRange.emptyRange.toString());
		ProvidedCapability groupCapability = new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_KIND, "group", new Version("1.0.0"));
		root.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(configurationIdentification, new Version(configurationVersion)), groupCapability});
		root.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_ECLIPSE);
		Map touchpointData = new HashMap();

		String configurationData = "";
		String unconfigurationData = "";

		ConfigData configData = info.getConfigData();
		if (configData != null) {
			for (Iterator iterator = configData.getFwDependentProps().entrySet().iterator(); iterator.hasNext();) {
				Entry aProperty = (Entry) iterator.next();
				String key = ((String) aProperty.getKey());
				if (key.equals("osgi.frameworkClassPath") || key.equals("osgi.framework") || key.equals("osgi.bundles") || key.equals("eof"))
					continue;
				configurationData += "setFwDependentProp(propName:" + key + ", propValue:" + ((String) aProperty.getValue()) + ");";
				unconfigurationData += "setFwDependentProp(propName:" + key + ", propValue:);";
			}
			for (Iterator iterator = configData.getFwIndependentProps().entrySet().iterator(); iterator.hasNext();) {
				Entry aProperty = (Entry) iterator.next();
				String key = ((String) aProperty.getKey());
				if (key.equals("osgi.frameworkClassPath") || key.equals("osgi.framework") || key.equals("osgi.bundles") || key.equals("eof"))
					continue;
				configurationData += "setFwIndependentProp(propName:" + key + ", propValue:" + ((String) aProperty.getValue()) + ");";
				unconfigurationData += "setFwIndependentProp(propName:" + key + ", propValue:);";
			}
		}

		LauncherData launcherData = info.getLauncherData();
		if (launcherData != null) {
			final String[] jvmArgs = launcherData.getJvmArgs();
			for (int i = 0; i < jvmArgs.length; i++) {
				configurationData += "addJvmArg(jvmArg:" + jvmArgs[i] + ");";
				unconfigurationData += "removeJvmArg(jvmArg:" + jvmArgs[i] + ");";
			}

			final String[] programArgs = launcherData.getProgramArgs();
			for (int i = 0; i < programArgs.length; i++) {
				String programArg = programArgs[i];
				if (programArg.equals("--launcher.library") || programArg.equals("-startup") || programArg.equals("-configuration"))
					i++;
				configurationData += "addProgramArg(programArg:" + programArg + ");";
				unconfigurationData += "removeProgramArg(programArg:" + programArg + ");";
			}
		}
		touchpointData.put("configure", configurationData);
		touchpointData.put("unconfigure", unconfigurationData);
		root.addTouchpointData(new TouchpointData(touchpointData));
		return MetadataFactory.createInstallableUnit(root);
	}

	public IStatus generate() {
		Set ius = new HashSet();

		Feature[] features = getFeatures(info.getFeaturesLocation());
		generateFeatureIUs(features, ius);

		BundleDescription[] bundles = getBundleDescriptions(info.getBundleLocations());
		generateBundleIUs(bundles, ius, info.getArtifactRepository());

		generateNativeIUs(info.getExecutableLocation(), ius, info.getArtifactRepository());

		generateConfigIUs(info.getConfigData() == null ? null : info.getConfigData().getBundles(), ius);

		if (info.addDefaultIUs())
			generateDefaultConfigIU(ius, info);

		generateRootIU(ius, info.getRootId(), info.getRootVersion());

		//		persistence.setMappingRules(info.getMappingRules() == null ? defaultMappingRules : info.getMappingRules());
		//		if (info.publishArtifacts() || info.publishArtifactRepository()) {
		//			persistence.saveArtifactRepository();
		//		}
		info.getMetadataRepository().addInstallableUnits((IInstallableUnit[]) ius.toArray(new IInstallableUnit[ius.size()]));

		return Status.OK_STATUS;
	}

	protected void generateBundleIUs(BundleDescription[] bundles, Set resultantIUs, IArtifactRepository destination) {
		for (int i = 0; i < bundles.length; i++) {
			BundleDescription bd = bundles[i];
			// A bundle may be null if the associated plug-in does not have a manifest file -
			// for example, org.eclipse.jdt.launching.j9
			if (bd != null) {
				String format = (String) ((Dictionary) bd.getUserObject()).get(BundleDescriptionFactory.BUNDLE_FILE_KEY);
				boolean isDir = format.equals(BundleDescriptionFactory.DIR) ? true : false;
				IArtifactKey key = MetadataGeneratorHelper.createEclipseArtifactKey(bd.getSymbolicName(), bd.getVersion().toString());
				IArtifactDescriptor ad = MetadataGeneratorHelper.createArtifactDescriptor(key, new File(bd.getLocation()), true, false);
				if (isDir)
					publishArtifact(ad, new File(bd.getLocation()).listFiles(), destination, false);
				else
					publishArtifact(ad, new File[] {new File(bd.getLocation())}, destination, true);
				IInstallableUnit iu = MetadataGeneratorHelper.createEclipseIU(bd, (Map) bd.getUserObject(), isDir, key);
				resultantIUs.add(iu);
			}
		}
	}

	protected void generateConfigIUs(BundleInfo[] infos, Set resultantIUs) {
		if (infos == null)
			return;
		for (int i = 0; i < infos.length; i++) {
			GeneratorBundleInfo bundle = new GeneratorBundleInfo(infos[i]);
			if (bundle.getSymbolicName().equals(ORG_ECLIPSE_UPDATE_CONFIGURATOR)) {
				bundle.setStartLevel(BundleInfo.NO_LEVEL);
				bundle.setMarkedAsStarted(false);
				bundle.setSpecialConfigCommands("addJvmArg(jvmArg:-Dorg.eclipse.update.reconcile=false);");
				bundle.setSpecialUnconfigCommands("removeJvmArg(jvmArg:-Dorg.eclipse.update.reconcile=false);");
			}
			if (bundle.getSymbolicName().equals(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR)) {
				bundle.setSpecialConfigCommands("addJvmArg(jvmArg:-Dorg.eclipse.equinox.simpleconfigurator.useReference=true);");
				bundle.setSpecialUnconfigCommands("removeJvmArg(jvmArg:-Dorg.eclipse.equinox.simpleconfigurator.useReference=true);");
			}
			IInstallableUnit cu = MetadataGeneratorHelper.createEclipseConfigurationUnit(bundle.getSymbolicName(), new Version(bundle.getVersion()), false, bundle, info.getFlavor(), null);
			if (cu != null)
				resultantIUs.add(cu);
		}

		if (info.addDefaultIUs()) {
			for (Iterator iterator = info.getDefaultIUs(resultantIUs).iterator(); iterator.hasNext();) {
				GeneratorBundleInfo bundle = (GeneratorBundleInfo) iterator.next();
				IInstallableUnit configuredIU = getIU(resultantIUs, bundle.getSymbolicName());
				if (configuredIU != null)
					bundle.setVersion(configuredIU.getVersion().toString());
				String filter = configuredIU == null ? null : configuredIU.getFilter();
				IInstallableUnit cu = MetadataGeneratorHelper.createEclipseConfigurationUnit(bundle.getSymbolicName(), new Version(bundle.getVersion()), false, bundle, info.getFlavor(), filter);
				//the configuration unit should share the same platform filter as the IU being configured.
				if (cu != null)
					resultantIUs.add(cu);
			}
		}
	}

	private void generateDefaultConfigIU(Set ius, IGeneratorInfo info) {
		//		TODO this is a bit of a hack.  We need to have the default IU fragment generated with code that configures
		//		and unconfigures.  the Generator should be decoupled from any particular provider but it is not clear
		//		 that we should add the create* methods to IGeneratorInfo...
		//		MockBundleDescription bd1 = new MockBundleDescription("defaultConfigure");
		//		MockBundleDescription bd2 = new MockBundleDescription("defaultUnconfigure");
		EclipseInstallGeneratorInfoProvider provider = (EclipseInstallGeneratorInfoProvider) info;
		ius.add(MetadataGeneratorHelper.createEclipseDefaultConfigurationUnit(provider.createDefaultConfigurationBundleInfo(), provider.createDefaultUnconfigurationBundleInfo(), info.getFlavor()));
	}

	/**
	 * This method generates IUs for the launchers found in the org.eclipse.executable feature, if present.
	 * @return <code>true</code> if the executable feature was processed successfully,
	 * and <code>false</code> otherwise.
	 */
	private boolean generateExecutableFeatureIUs(Set resultantIUs, IArtifactRepository destination) {
		File parentDir = info.getFeaturesLocation();
		if (!parentDir.exists())
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
		File binDir = new File(executableFeatureDir, "bin");
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
					generateExecutableIUs(ws, os, arch, versionString, archDirs[archIndex], resultantIUs, destination);
				}
			}
		}
		return true;
	}

	/**
	 * Generates IUs and CUs for the files that make up the launcher for a given
	 * ws/os/arch combination.
	 */
	private void generateExecutableIUs(String ws, String os, String arch, String version, File root, Set resultantIUs, IArtifactRepository destination) {
		//Create the IU
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(true);
		String launcherId = "org.eclipse.launcher." + ws + '.' + os + '.' + arch; //$NON-NLS-1$
		iu.setId(launcherId);
		Version launcherVersion = new Version(version);
		iu.setVersion(launcherVersion);
		String filter = "(& (osgi.ws=" + ws + ") (osgi.os=" + os + ") (osgi.arch=" + arch + "))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		iu.setFilter(filter);

		IArtifactKey key = MetadataGeneratorHelper.createLauncherArtifactKey(launcherId, launcherVersion);
		iu.setArtifacts(new IArtifactKey[] {key});
		iu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_NATIVE);
		resultantIUs.add(MetadataFactory.createInstallableUnit(iu));

		//Create the CU
		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		cu.setId(info.getFlavor() + launcherId);
		cu.setVersion(launcherVersion);
		cu.setFilter(filter);
		cu.setHost(launcherId, new VersionRange(launcherVersion, true, launcherVersion, true));

		mungeLauncherFileNames(root);

		cu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_NATIVE);
		Map touchpointData = new HashMap();
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		if (!"win32".equals(os)) { //$NON-NLS-1$
			File[] launcherFiles = root.listFiles();
			for (int i = 0; i < launcherFiles.length; i++) {
				configurationData += " chmod(targetDir:${installFolder}, targetFile:" + launcherFiles[i].getName() + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		touchpointData.put("install", configurationData); //$NON-NLS-1$
		cu.addTouchpointData(new TouchpointData(touchpointData));
		resultantIUs.add(MetadataFactory.createInstallableUnit(cu));

		//Create the artifact descriptor
		IArtifactDescriptor descriptor = MetadataGeneratorHelper.createArtifactDescriptor(key, root, false, true);
		publishArtifact(descriptor, root.listFiles(), destination, false);
	}

	protected void generateFeatureIUs(Feature[] features, Set resultantIUs) {
		for (int i = 0; i < features.length; i++) {
			Feature feature = features[i];
			resultantIUs.add(MetadataGeneratorHelper.createGroupIU(feature));
		}
	}

	protected void generateNativeIUs(File executableLocation, Set resultantIUs, IArtifactRepository destination) {
		//generate data for JRE
		File jreLocation = info.getJRELocation();
		IArtifactDescriptor artifact = MetadataGeneratorHelper.createJREData(jreLocation, resultantIUs);
		publishArtifact(artifact, new File[] {jreLocation}, destination, false);

		//If the executable feature is present, use it to generate IUs for launchers
		if (generateExecutableFeatureIUs(resultantIUs, destination) || executableLocation == null)
			return;

		//generate data for executable launcher
		artifact = MetadataGeneratorHelper.createLauncherIU(executableLocation, info.getFlavor(), resultantIUs);
		File[] launcherFiles = null;
		//hard-coded name is ok, since console launcher is not branded, and appears on Windows only
		File consoleLauncher = new File(executableLocation.getParentFile(), "eclipsec.exe"); //$NON-NLS-1$
		if (consoleLauncher.exists())
			launcherFiles = new File[] {executableLocation, consoleLauncher};
		else
			launcherFiles = new File[] {executableLocation};
		publishArtifact(artifact, launcherFiles, destination, false);
	}

	protected void generateRootIU(Set resultantIUs, String rootIUId, String rootIUVersion) {
		if (rootIUId == null)
			return;
		resultantIUs.add(createTopLevelIU(resultantIUs, rootIUId, rootIUVersion));
	}

	protected BundleDescription[] getBundleDescriptions(File[] bundleLocations) {
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
				File location = new File(FileLocator.toFileURL(Activator.getContext().getBundle().getEntry(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR + ".jar")).getFile());
				result[result.length - 1] = factory.getBundleDescription(location);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	protected BundleDescriptionFactory getBundleFactory() {
		BundleDescriptionFactory factory = new BundleDescriptionFactory(stateObjectFactory, null);
		return factory;
	}

	private Feature[] getFeatures(File folder) {
		if (folder == null || !folder.exists())
			return new Feature[0];
		File[] locations = folder.listFiles();
		ArrayList result = new ArrayList(locations.length);
		for (int i = 0; i < locations.length; i++) {
			Feature feature = new FeatureParser().parse(locations[i]);
			if (feature != null)
				result.add(feature);
		}
		return (Feature[]) result.toArray(new Feature[result.size()]);
	}

	protected IGeneratorInfo getGeneratorInfo() {
		return info;
	}

	private IInstallableUnit getIU(Set ius, String id) {
		for (Iterator iterator = ius.iterator(); iterator.hasNext();) {
			IInstallableUnit tmp = (IInstallableUnit) iterator.next();
			if (tmp.getId().equals(id))
				return tmp;
		}
		return null;
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
		if (descriptor == null)
			return;
		if (!info.publishArtifacts()) {
			destination.addDescriptor(descriptor);
			return;
		}
		if (asIs && files.length == 1) {
			try {
				if (!destination.contains(descriptor)) {
					OutputStream output = destination.getOutputStream(descriptor);
					if (output == null)
						throw new IOException("unable to open output stream for " + descriptor);
					FileUtils.copyStream(new BufferedInputStream(new FileInputStream(files[0])), true, new BufferedOutputStream(output), true);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			File tempFile = null;
			try {
				tempFile = File.createTempFile("p2.generator", "");
				FileUtils.zip(files, tempFile);
				if (!destination.contains(descriptor)) {
					OutputStream output = destination.getOutputStream(descriptor);
					if (output == null)
						throw new IOException("unable to open output stream for " + descriptor);
					FileUtils.copyStream(new BufferedInputStream(new FileInputStream(tempFile)), true, new BufferedOutputStream(output), true);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (tempFile != null)
					tempFile.delete();
			}
		}
	}
}
