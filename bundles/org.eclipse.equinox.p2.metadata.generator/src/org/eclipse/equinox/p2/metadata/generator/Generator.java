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
import java.net.MalformedURLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.internal.p2.metadata.generator.Activator;
import org.eclipse.equinox.internal.p2.metadata.generator.features.FeatureParser;
import org.eclipse.equinox.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Version;

public class Generator {

	private static final Version ECLIPSE_TOUCHPOINT_VERSION = new Version(1, 0, 0);
	private static final String ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR = "org.eclipse.equinox.simpleconfigurator";
	private static final String ORG_ECLIPSE_UPDATE_CONFIGURATOR = "org.eclipse.update.configurator";
	//	private static String[][] defaultMappingRules = new String[][] { {"(& (namespace=eclipse) (classifier=feature))", "${repoUrl}/feature/${id}_${version}"}, {"(& (namespace=eclipse) (classifier=plugin))", "${repoUrl}/plugin/${id}_${version}"}, {"(& (namespace=eclipse) (classifier=native))", "${repoUrl}/native/${id}_${version}"}};

	private StateObjectFactory stateObjectFactory;
	private IGeneratorInfo info;

	public Generator(IGeneratorInfo infoProvider) {
		this.info = infoProvider;
		// TODO need to figure a better way of configuring the generator...
		PlatformAdmin platformAdmin = (PlatformAdmin) ServiceHelper.getService(Activator.getContext(), PlatformAdmin.class.getName());
		if (platformAdmin != null) {
			stateObjectFactory = platformAdmin.getFactory();
		}
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
		info.getMetadataRepository().addInstallableUnits((InstallableUnit[]) ius.toArray(new InstallableUnit[ius.size()]));

		return Status.OK_STATUS;
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

	private Feature[] getFeatures(File folder) {
		if (folder == null || !folder.exists())
			return new Feature[0];
		File[] locations = folder.listFiles();
		ArrayList result = new ArrayList(locations.length);
		for (int i = 0; i < locations.length; i++) {
			if (locations[i].isDirectory()) {
				FeatureParser parser = new FeatureParser();
				//skip directories that don't contain a feature.xml file
				File file = new File(locations[i], "feature.xml"); //$NON-NLS-1$
				if (file.exists()) {
					try {
						Feature feature = parser.parse(file.toURL());
						if (feature != null)
							result.add(feature);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			} else if (locations[i].getName().endsWith(".jar")) {
				Feature feature = getFeatureFromJAR(locations[i]);
				if (feature != null)
					result.add(feature);
			}
		}
		return (Feature[]) result.toArray(new Feature[result.size()]);
	}

	private Feature getFeatureFromJAR(File file) {
		if (!file.exists())
			return null;
		FeatureParser parser = new FeatureParser();
		try {
			JarFile jar = new JarFile(file);
			JarEntry entry = jar.getJarEntry("feature.xml");
			return entry == null ? null : parser.parse(jar.getInputStream(entry));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected void generateFeatureIUs(Feature[] features, Set resultantIUs) {
		for (int i = 0; i < features.length; i++) {
			Feature feature = features[i];
			resultantIUs.add(MetadataGeneratorHelper.createGroupIU(feature));
		}
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

	protected void generateRootIU(Set resultantIUs, String rootIUId, String rootIUVersion) {
		if (rootIUId == null)
			return;
		resultantIUs.add(createTopLevelIU(resultantIUs, rootIUId, rootIUVersion));
	}

	protected void generateNativeIUs(File executableLocation, Set resultantIUs, IArtifactRepository destination) {
		if (executableLocation == null)
			return;
		HashSet newArtifacts = new HashSet();

		//generate data for JRE
		File jreLocation = new File(executableLocation.getParentFile(), "jre");
		MetadataGeneratorHelper.createJREData(jreLocation, resultantIUs, newArtifacts);
		publishArtifact(jreLocation, newArtifacts);
		newArtifacts.clear();

		//generate data for executable launcher
		MetadataGeneratorHelper.createLauncherData(executableLocation, info.getFlavor(), resultantIUs, newArtifacts);
		publishArtifact(executableLocation, newArtifacts);
		newArtifacts.clear();

		//generate data for eclipsec.exe if applicable
		//hard-coded name is ok, since console launcher is not branded, and appears on Windows only
		File consoleLauncher = new File(executableLocation.getParentFile(), "eclipsec.exe");
		if (consoleLauncher.exists()) {
			MetadataGeneratorHelper.createLauncherData(consoleLauncher, info.getFlavor(), resultantIUs, newArtifacts);
			publishArtifact(consoleLauncher, newArtifacts);
		}
	}

	private void publishArtifact(File location, Set artifacts) {
		if (!info.publishArtifacts() || artifacts.isEmpty())
			return;
		for (Iterator i = artifacts.iterator(); i.hasNext();) {
			IArtifactDescriptor descriptor = (IArtifactDescriptor) i.next();
			publishArtifact(descriptor, location, info.getArtifactRepository(), false, true);
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
			IInstallableUnit cu = MetadataGeneratorHelper.createEclipseConfigurationUnit(bundle.getSymbolicName(), new Version(bundle.getVersion()), false, bundle, info.getFlavor());
			if (cu != null)
				resultantIUs.add(cu);
		}

		if (info.addDefaultIUs()) {
			for (Iterator iterator = info.getDefaultIUs(resultantIUs).iterator(); iterator.hasNext();) {
				GeneratorBundleInfo bundle = (GeneratorBundleInfo) iterator.next();
				InstallableUnit configuredIU = getIU(resultantIUs, bundle.getSymbolicName());
				if (configuredIU != null)
					bundle.setVersion(configuredIU.getVersion().toString());
				IInstallableUnit cu = MetadataGeneratorHelper.createEclipseConfigurationUnit(bundle.getSymbolicName(), new Version(bundle.getVersion()), false, bundle, info.getFlavor());
				if (cu != null)
					resultantIUs.add(cu);
			}
		}
	}

	private InstallableUnit getIU(Set ius, String id) {
		for (Iterator iterator = ius.iterator(); iterator.hasNext();) {
			InstallableUnit tmp = (InstallableUnit) iterator.next();
			if (tmp.getId().equals(id))
				return tmp;
		}
		return null;
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
				if (info.publishArtifacts())
					//				publishArtifact(info.getArtifactRepoLocation().getParentFile(), new File(bd.getLocation()), key.getClassifier(), key.getId() + "_" + key.getVersion().toString(), !isDir, true);
					publishArtifact(ad, new File(bd.getLocation()), destination, !isDir, true);
				else
					destination.addDescriptor(ad);

				IInstallableUnit iu = MetadataGeneratorHelper.createEclipseIU(bd, (Map) bd.getUserObject(), isDir, key);
				resultantIUs.add(iu);
			}
		}
	}

	protected InstallableUnit createTopLevelIU(Set resultantIUs, String configurationIdentification, String configurationVersion) {
		InstallableUnit iu = new InstallableUnit();
		iu.setSingleton(true);
		iu.setId(configurationIdentification);
		iu.setVersion(new Version(configurationVersion));

		ArrayList reqsConfigurationUnits = new ArrayList(resultantIUs.size());
		for (Iterator iterator = resultantIUs.iterator(); iterator.hasNext();) {
			InstallableUnit tmp = (InstallableUnit) iterator.next();
			reqsConfigurationUnits.add(RequiredCapability.createRequiredCapabilityForName(tmp.getId(), new VersionRange(tmp.getVersion(), true, tmp.getVersion(), true), false));
		}
		iu.setRequiredCapabilities((RequiredCapability[]) reqsConfigurationUnits.toArray(new RequiredCapability[reqsConfigurationUnits.size()]));
		iu.setApplicabilityFilter("");
		iu.setArtifacts(new IArtifactKey[0]);

		iu.setProperty("lineUp", "true");
		iu.setProperty(IInstallableUnitConstants.UPDATE_FROM, configurationIdentification);
		iu.setProperty(IInstallableUnitConstants.UPDATE_RANGE, VersionRange.emptyRange.toString());
		ProvidedCapability groupCapability = new ProvidedCapability(IInstallableUnit.IU_KIND_NAMESPACE, "group", new Version("1.0.0"));
		iu.setCapabilities(new ProvidedCapability[] {groupCapability});
		iu.setTouchpointType(new TouchpointType("eclipse", ECLIPSE_TOUCHPOINT_VERSION));
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
		iu.setImmutableTouchpointData(new TouchpointData(touchpointData));
		return iu;
	}

	// Put the artifact on the server
	protected void publishArtifact(IArtifactDescriptor descriptor, File bundlePath, IArtifactRepository destination, boolean asIs, boolean recurse) {
		//		key.getClassifier(), key.getId() + '_' + key.getVersion().toString()
		if (asIs) {
			try {
				if (!destination.contains(descriptor)) {
					OutputStream output = destination.getOutputStream(descriptor);
					if (output == null)
						throw new IOException("unable to open output stream for " + descriptor);
					FileUtils.copyStream(new BufferedInputStream(new FileInputStream(bundlePath)), true, new BufferedOutputStream(output), true);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			File tempFile = null;
			try {
				tempFile = File.createTempFile("work", "");
				if (recurse)
					FileUtils.zip(new File[] {bundlePath}, tempFile);
				else
					FileUtils.zip(bundlePath.listFiles(new FileFilter() {
						public boolean accept(File pathname) {
							if (pathname.isFile())
								return true;
							return false;
						}
					}), tempFile);
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

	protected IGeneratorInfo getGeneratorInfo() {
		return info;
	}
}
