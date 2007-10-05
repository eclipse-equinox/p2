/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.generator;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.internal.p2.metadata.generator.Activator;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class EclipseInstallGeneratorInfoProvider implements IGeneratorInfo {
	private static final String ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR = "org.eclipse.equinox.simpleconfigurator";

	private final static String FILTER_OBJECTCLASS = "(" + Constants.OBJECTCLASS + "=" + FrameworkAdmin.class.getName() + ")";
	private final static String filterFwName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_NAME + "=Equinox)";
	//String filterFwVersion = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_VERSION + "=" + props.getProperty("equinox.fw.version") + ")";
	private final static String filterLauncherName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME + "=Eclipse.exe)";
	//String filterLauncherVersion = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_VERSION + "=" + props.getProperty("equinox.launcher.version") + ")";
	private final static String frameworkAdminFillter = "(&" + FILTER_OBJECTCLASS + filterFwName + filterLauncherName + ")";

	private ServiceTracker frameworkAdminTracker;
	private Manipulator manipulator;
	private ArrayList defaultIUs;

	private File baseLocation;
	private File[] bundleLocations;
	private File featuresLocation;
	private File configLocation;
	private File executableLocation;
	private String flavor;
	private String rootId;
	private String rootVersion;
	private boolean publishArtifacts = false;
	private boolean publishArtifactRepo = false;
	private boolean append = false;
	private String[][] mappingRules;
	private boolean addDefaultIUs = true;
	private IMetadataRepository metadataRepository;
	private IArtifactRepository artifactRepository;
	private static String os;

	public EclipseInstallGeneratorInfoProvider() {
		super();
	}

	/**
	 * Returns a default name for the executable.
	 */
	public static String getDefaultExecutableName() {
		String theOS = os;
		if (theOS == null) {
			EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
			theOS = info.getOS();
		}
		if (theOS.equalsIgnoreCase("win32")) //$NON-NLS-1$
			return "eclipse.exe"; //$NON-NLS-1$
		//FIXME Is this a reasonable default for all non-Windows platforms?
		return "eclipse"; //$NON-NLS-1$
	}

	public void initialize(File base) {
		initialize(base, new File(base, "configuration"), new File(base, getDefaultExecutableName()), new File[] {new File(base, "plugins")}, new File(base, "features"));
	}

	public void initialize(File base, File config, File executable, File[] bundleLocations, File features) {
		// TODO
		if (base == null || !base.exists())
			throw new RuntimeException("Source directory is invalid: " + base.getAbsolutePath());
		this.baseLocation = base;
		if (config == null || config.exists())
			this.configLocation = config;
		if (executable == null || executable.exists())
			this.executableLocation = executable;
		this.bundleLocations = bundleLocations;
		this.featuresLocation = features;

		expandBundleLocations();

		// if the config or exe are not set then we cannot be generating any data related to the config so 
		// don't bother setting up the manipulator.  In fact, the manipulator will likely be invalid without
		// these locations.
		if (configLocation == null || executableLocation == null)
			return;

		createFrameworkManipulator();

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwPersistentDataLocation(configLocation, true);
		launcherData.setLauncher(executableLocation);
		try {
			manipulator.load();
		} catch (IllegalStateException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (FrameworkAdminRuntimeException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}

	private void expandBundleLocations() {
		ArrayList result = new ArrayList();
		for (int i = 0; i < bundleLocations.length; i++) {
			File location = bundleLocations[i];
			if (location.isDirectory()) {
				File[] list = location.listFiles();
				for (int j = 0; j < list.length; j++)
					result.add(list[j]);
			} else {
				result.add(location);
			}
		}
		bundleLocations = (File[]) result.toArray(new File[result.size()]);
	}

	public ArrayList getDefaultIUs(Set ius) {
		if (defaultIUs != null)
			return defaultIUs;
		defaultIUs = new ArrayList(5);
		if (addDefaultIUs) {
			defaultIUs.addAll(createLauncherBundleInfo(ius));
			defaultIUs.add(createLauncher());
			defaultIUs.add(createSimpleConfigurator());
			//			defaultIUs.add(createDefaultConfigurationBundleInfo());
			//			defaultIUs.add(createDefaultUnconfigurationBundleInfo());
		}
		return defaultIUs;
	}

	protected GeneratorBundleInfo createDefaultConfigurationBundleInfo() {
		GeneratorBundleInfo result = new GeneratorBundleInfo();
		result.setSymbolicName("defaultConfigure");
		result.setVersion("1.0.0");
		result.setStartLevel(4);
		result.setSpecialConfigCommands("manipulator.getConfigData().addBundle(bundleToInstall);");
		return result;
	}

	protected GeneratorBundleInfo createDefaultUnconfigurationBundleInfo() {
		GeneratorBundleInfo result = new GeneratorBundleInfo();
		result.setSymbolicName("defaultUnconfigure");
		result.setVersion("1.0.0");
		result.setSpecialConfigCommands("manipulator.getConfigData().removeBundle(bundleToRemove);");
		return result;
	}

	/**
	 * Obtains the framework manipulator instance. Throws an exception
	 * if it could not be created.
	 */
	private void createFrameworkManipulator() {
		FrameworkAdmin admin = getFrameworkAdmin();
		if (admin == null)
			throw new RuntimeException("Framework admin service not found"); //$NON-NLS-1$
		manipulator = admin.getManipulator();
		if (manipulator == null)
			throw new RuntimeException("Framework manipulator not found"); //$NON-NLS-1$
	}

	private FrameworkAdmin getFrameworkAdmin() {
		if (frameworkAdminTracker == null) {
			try {
				Filter filter = Activator.getContext().createFilter(frameworkAdminFillter);
				frameworkAdminTracker = new ServiceTracker(Activator.getContext(), filter, null);
				frameworkAdminTracker.open();
			} catch (InvalidSyntaxException e) {
				// never happens
				e.printStackTrace();
			}
		}
		//		try {
		//			frameworkAdminTracker.waitForService(500);
		//		} catch (InterruptedException e) {
		//			// ignore
		//		}
		return (FrameworkAdmin) frameworkAdminTracker.getService();
	}

	public ConfigData getConfigData() {
		return manipulator == null ? null : manipulator.getConfigData();
	}

	public LauncherData getLauncherData() {
		return manipulator == null ? null : manipulator.getLauncherData();
	}

	private Collection createLauncherBundleInfo(Set ius) {
		Collection result = new HashSet();
		Collection launchers = getIUs(ius, "org.eclipse.equinox.launcher.");
		for (Iterator iterator = launchers.iterator(); iterator.hasNext();) {
			InstallableUnit object = (InstallableUnit) iterator.next();
			GeneratorBundleInfo temp = new GeneratorBundleInfo();
			temp.setSymbolicName(object.getId());
			temp.setVersion(object.getVersion().toString());
			temp.setSpecialConfigCommands("manipulator.getLauncherData().addProgramArg('--launcher.library');manipulator.getLauncherData().addProgramArg(artifact);");
			result.add(temp);
		}
		return result;
	}

	private Collection getIUs(Set ius, String prefix) {
		Set result = new HashSet();
		for (Iterator iterator = ius.iterator(); iterator.hasNext();) {
			IInstallableUnit tmp = (IInstallableUnit) iterator.next();
			if (tmp.getId().startsWith(prefix))
				result.add(tmp);
		}
		return result;
	}

	private GeneratorBundleInfo createLauncher() {
		GeneratorBundleInfo result = new GeneratorBundleInfo();
		result.setSymbolicName("org.eclipse.equinox.launcher");
		result.setVersion("0.0.0");
		//result.setSpecialConfigCommands("manipulator.addProgramArgument('-startup'); manipulator.addProgramArgument(artifact);");
		result.setSpecialConfigCommands("manipulator.getLauncherData().addProgramArg('-startup');manipulator.getLauncherData().addProgramArg(artifact);");
		return result;
	}

	private GeneratorBundleInfo createSimpleConfigurator() {
		GeneratorBundleInfo result = new GeneratorBundleInfo();
		result.setSymbolicName(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR);
		result.setVersion("0.0.0");
		result.setStartLevel(1);
		result.setMarkedAsStarted(true);
		result.setSpecialConfigCommands("manipulator.getLauncherData().addJvmArg('-Dorg.eclipse.equinox.simpleconfigurator.useReference=true');");
		return result;
	}

	public File getBaseLocation() {
		return baseLocation;
	}

	public File[] getBundleLocations() {
		return bundleLocations;
	}

	public File getFeaturesLocation() {
		return featuresLocation;
	}

	public File getConfigurationLocation() {
		return configLocation;
	}

	public File getExecutableLocation() {
		return executableLocation;
	}

	public boolean publishArtifacts() {
		return publishArtifacts;
	}

	public boolean addDefaultIUs() {
		return addDefaultIUs;
	}

	public boolean publishArtifactRepository() {
		return publishArtifactRepo;
	}

	public String getRootId() {
		return rootId;
	}

	public String getRootVersion() {
		if (rootVersion == null)
			return "0.0.0"; //$NON-NLS-1$
		return rootVersion;
	}

	public String getFlavor() {
		return flavor;
	}

	public void setMetadataRepository(IMetadataRepository value) {
		metadataRepository = value;
	}

	public void setArtifactRepository(IArtifactRepository value) {
		artifactRepository = value;
	}

	public void setFlavor(String value) {
		flavor = value;
	}

	public void setRootId(String value) {
		rootId = value;
	}

	public void setRootVersion(String value) {
		rootVersion = value;
	}

	public void setPublishArtifacts(boolean value) {
		publishArtifacts = value;
	}

	public void setPublishArtifactRepository(boolean value) {
		publishArtifactRepo = value;
	}

	public boolean append() {
		return append;
	}

	public String[][] getMappingRules() {
		return mappingRules;
	}

	public void setMappingRules(String[][] value) {
		mappingRules = value;
	}

	public void setExecutableLocation(String value) {
		executableLocation = new File(value);
	}

	public void setAppend(boolean value) {
		append = value;
	}

	public void setAddDefaultIUs(boolean value) {
		addDefaultIUs = value;
	}

	public IArtifactRepository getArtifactRepository() {
		return artifactRepository;
	}

	public IMetadataRepository getMetadataRepository() {
		return metadataRepository;
	}

	public void setOS(String os) {
		this.os = os;
	}
}
