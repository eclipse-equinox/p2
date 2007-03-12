/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.frameworkadmin.equinox.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import org.eclipse.equinox.configuratormanipulator.ConfiguratorManipulator;
import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.frameworkadmin.equinox.internal.utils.FileUtils;
import org.eclipse.equinox.frameworkadmin.equinox.internal.utils.state.BundleHelper;
import org.eclipse.equinox.internal.frameworkadmin.utils.SimpleBundlesState;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

public class EquinoxManipulatorImpl implements Manipulator {
	private static final long DEFAULT_LASTMODIFIED = 0L;
	public static final String FW_NAME = "Equinox";
	public static final String FW_VERSION = "3.3M5";
	public static final String LAUCNHER_NAME = "Eclipse.exe";
	public static final String LAUNCHER_VERSION = "3.2";
	private static final boolean LOG_ILLEGALSTATEEXCEPTION = false;

	/**
	 * If the fwConfigLocation is a file and its name does not equal "config.ini",
	 * throw an IllegaStateException. 
	 * If the fwConfigLocation is a file and its name equals "config.ini",
	 * fwConfigLocation will be updated by its parent directory.
	 * 
	 * Then, reset fwConfigLocation and fwPersistentDataLocation to be matched.
	 * 
	 * @param launcherData
	 */
	static void checkConsistencyOfFwConfigLocAndFwPersistentDataLoc(LauncherData launcherData) {
		File fwConfigLocation = launcherData.getFwConfigLocation();
		File fwPersistentDataLocation = launcherData.getFwPersistentDataLocation();

		if (fwConfigLocation != null) {
			if (fwConfigLocation.isFile()) {
				if (fwConfigLocation.getName().equals(EquinoxConstants.CONFIG_INI))
					fwConfigLocation = fwConfigLocation.getParentFile();
				else
					throw new IllegalStateException("fwConfigLocation is not a directory but its name does NOT equal \"" + EquinoxConstants.CONFIG_INI + "\"!\n" + "\tfwConfigLocation=" + fwConfigLocation.getAbsolutePath() + "\n\t,fwPersistentDataLocation=" + fwPersistentDataLocation.getAbsolutePath());
				launcherData.setFwConfigLocation(fwConfigLocation);
			}
			if (fwPersistentDataLocation != null) {
				//				Log.log(LogService.LOG_DEBUG, "fwConfigLocation=" + fwConfigLocation.getAbsolutePath() + ",\n\tfwInstancePrivateArea=" + fwPersistentDataLocation.getAbsolutePath());
				//if (!fwConfigLocation.getParentFile().equals(fwPersistentDataLocation))
				//throw new IllegalStateException("!configFile.getParentFile().equals(fwInstancePrivateArea)\n" + "\tconfigFile=" + fwConfigLocation.getAbsolutePath() + "\n\t,fwInstancePrivateArea=" + fwPersistentDataLocation.getAbsolutePath());
				if (!fwConfigLocation.equals(fwPersistentDataLocation))
					throw new IllegalStateException("!fwConfigLocation.equals(fwPersistentDataLocation)\n" + "\t!fwConfigLocation=" + fwConfigLocation.getAbsolutePath() + "\n\t,fwPersistentDataLocation=" + fwPersistentDataLocation.getAbsolutePath());
			} else
				launcherData.setFwPersistentDataLocation(fwConfigLocation, launcherData.isClean());
			//launcherData.setFwPersistentDataLocation(fwConfigLocation.getParentFile(), launcherData.isClean());
		} else {
			if (fwPersistentDataLocation != null) {
				launcherData.setFwConfigLocation(fwPersistentDataLocation);
				//launcherData.setFwConfigLocation(new File(fwPersistentDataLocation, EquinoxConstants.CONFIG_INI));
			} else {
				File home = launcherData.getHome();
				if (home == null)
					throw new IllegalStateException("All of fwConfigLocation, fwPersistentDataLocation, and home are not set");
				fwConfigLocation = new File(home, "configuration");
				launcherData.setFwPersistentDataLocation(fwConfigLocation, launcherData.isClean());
				launcherData.setFwConfigLocation(fwConfigLocation);
			}
		}
	}

	static File getLauncherConfigLocation(LauncherData launcherData) {
		File launcherConfigLocation = launcherData.getLauncherConfigLocation();
		if (launcherConfigLocation != null)
			return launcherConfigLocation;

		File launcher = launcherData.getLauncher();
		if (launcher == null)
			return null;
		String launcherName = launcher.getName();
		launcherName = launcherName.substring(0, launcherName.lastIndexOf("."));
		return new File(launcher.getParent() + File.separator + launcherName + EquinoxConstants.INI_EXTENSION);
	}

	ConfigData configData = new ConfigData(EquinoxConstants.FW_NAME, EquinoxConstants.FW_VERSION, EquinoxConstants.LAUNCHER_NAME, EquinoxConstants.LAUNCHER_VERSION);
	LauncherData launcherData = new LauncherData(EquinoxConstants.FW_NAME, EquinoxConstants.FW_VERSION, EquinoxConstants.LAUNCHER_NAME, EquinoxConstants.LAUNCHER_VERSION);

	BundleContext context = null;
	private Properties platformProperties = new Properties();

	ServiceTracker cmTracker;
	int trackingCount = -1;

	//	private final boolean runtime;

	ConfiguratorManipulator configuratorManipulator;

	EquinoxFwAdminImpl fwAdmin = null;

	EquinoxManipulatorImpl(BundleContext context, EquinoxFwAdminImpl fwAdmin) {
		this(context, fwAdmin, false);
	}

	EquinoxManipulatorImpl(BundleContext context, EquinoxFwAdminImpl fwAdmin, boolean runtime) {
		this.context = context;
		this.fwAdmin = fwAdmin;
		if (context != null) {
			cmTracker = new ServiceTracker(context, ConfiguratorManipulator.class.getName(), null);
			cmTracker.open();
		}
		//		this.runtime = runtime;
		if (runtime)
			initializeRuntime();
		// XXX For Equinox, default value of Initial Bundle Start Level is 4.
		// Precisely speaking, it's not correct. 
		// Equinox doesn't support setting initial bundle start level as an OSGi terminology.
		// Only bundles installed by config.ini and updateconfigurator will have that start level(4).
		// Others has a start level of 1.
		configData.setInitialBundleStartLevel(4);
	}

	public BundlesState getBundlesState() throws FrameworkAdminRuntimeException {
		if (context == null)
			return new SimpleBundlesState(fwAdmin, this, EquinoxConstants.FW_SYMBOLIC_NAME);

		if (!EquinoxBundlesState.checkFullySupported())
			return new SimpleBundlesState(fwAdmin, this, EquinoxConstants.FW_SYMBOLIC_NAME);

		if (platformProperties.isEmpty())
			return new EquinoxBundlesState(context, fwAdmin, this, false);
		// XXX checking if fwDependent or fwIndependent platformProperties are updated after the platformProperties was created might be required for better implementation.		
		return new EquinoxBundlesState(context, fwAdmin, this, platformProperties);
	}

	public ConfigData getConfigData() throws FrameworkAdminRuntimeException {
		return configData;
	}

	public BundleInfo[] getExpectedState() throws IllegalArgumentException, IOException, FrameworkAdminRuntimeException {
		//Log.log(LogService.LOG_DEBUG, this, "getExpectedState()", "BEGIN");
		SimpleBundlesState.checkAvailability(fwAdmin);

		BundlesState bundleState = this.getBundlesState();
		if (bundleState instanceof SimpleBundlesState)
			return new BundleInfo[0];
		bundleState.resolve(true);

		return bundleState.getExpectedState();
	}

	public LauncherData getLauncherData() throws FrameworkAdminRuntimeException {
		return launcherData;
	}

	/**
	 * Return the configuration location.
	 * 
	 * @see Location
	 */
	private File getRunningConfigurationLocation() {
		ServiceTracker tracker = null;
		Filter filter = null;
		try {
			filter = context.createFilter(Location.CONFIGURATION_FILTER);
		} catch (InvalidSyntaxException e) {
			// ignore this. It should never happen as we have tested the above format.
		}
		tracker = new ServiceTracker(context, filter, null);
		tracker.open();
		Location location = (Location) tracker.getService();
		URL url = location.getURL();
		if (!url.getProtocol().equals("file"))
			return null;
		return new File(url.getFile());
	}

	private File getRunningLauncherFile() {
		File launcherFile = null;
		String eclipseCommandsSt = context.getProperty(EquinoxConstants.PROP_ECLIPSE_COMMANDS);
		if (eclipseCommandsSt == null)
			return null;

		StringTokenizer tokenizer = new StringTokenizer(eclipseCommandsSt, "\n");
		boolean found = false;
		String launcherSt = null;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (found) {
				launcherSt = token;
				break;
			}
			if (token.equals("-launcher"))
				found = true;
		}
		if (launcherSt != null)
			launcherFile = new File(launcherSt);
		return launcherFile;
	}

	private Properties getRunningPlatformProperties() {
		Properties props = new Properties();
		for (int i = 0; i < EquinoxBundlesState.PROPS.length; i++) {
			String value = context.getProperty(EquinoxBundlesState.PROPS[i]);
			if (value != null)
				props.setProperty(EquinoxBundlesState.PROPS[i], value);
		}
		return props;
	}

	public long getTimeStamp() {
		long ret = this.getTimeStampWithoutFwPersistentData();
		if (this.launcherData.isClean())
			return ret;
		long lastModifiedFwPersistent = EquinoxBundlesState.getTimeStamp(launcherData.getFwPersistentDataLocation());
		return Math.max(ret, lastModifiedFwPersistent);
	}

	private long getTimeStampWithoutFwPersistentData() {
		SimpleBundlesState.checkAvailability(fwAdmin);
		File launcherConfigFile = getLauncherConfigLocation(launcherData);
		long lastModifiedLauncherConfigFile = DEFAULT_LASTMODIFIED;
		long lastModifiedFwConfigFile = DEFAULT_LASTMODIFIED;
		if (launcherConfigFile != null) {
			// use launcher. -- > load from LaucnherConfig file.
			lastModifiedLauncherConfigFile = launcherConfigFile.lastModified();
		}
		checkConsistencyOfFwConfigLocAndFwPersistentDataLoc(launcherData);

		if (launcherData.getFwConfigLocation() != null) {
			File fwConfigFile = new File(launcherData.getFwConfigLocation(), EquinoxConstants.CONFIG_INI);
			lastModifiedFwConfigFile = fwConfigFile.lastModified();
		}
		long ret = Math.max(lastModifiedLauncherConfigFile, lastModifiedFwConfigFile);
		return ret;
	}

	//	// 
	//	public void load() throws IllegalStateException, IOException, FrameworkAdminRuntimeException {
	//		this.load(true);
	//	}

	public void initialize() {
		Log.log(LogService.LOG_DEBUG, this, "initialize()", "BEGIN");
		configData.initialize();
		launcherData.initialize();
	}

	private void initializeRuntime() {
		//TODO refine the implementation. using some MAGIC dependent on Eclipse.exe and Equinox implementation,
		// set parameters according to the current running fw.

		// 1. retrieve location data from Location services registered by equinox fw.
		String fwJarLocation = context.getProperty(EquinoxConstants.PROP_OSGI_FW);
		if (!fwJarLocation.startsWith("file:"))
			throw new IllegalStateException("Current implementation assume that property value keyed by " + EquinoxConstants.PROP_OSGI_FW + " must start with \"file:\". But it was not:" + fwJarLocation);
		File fwJar = new File(fwJarLocation.substring("file:".length()));
		//System.out.println("fwJar=" + fwJar);
		File fwConfigLocation = getRunningConfigurationLocation();
		File launcherFile = getRunningLauncherFile();
		launcherData.setFwJar(fwJar);
		launcherData.setFwPersistentDataLocation(fwConfigLocation, false);
		launcherData.setLauncher(launcherFile);
		try {
			this.loadWithoutFwPersistentData();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// 2. Create a Manipulator object fully initialized to the current running fw.

		ServiceReference reference = context.getServiceReference(StartLevel.class.getName());
		StartLevel startLevel = (StartLevel) context.getService(reference);
		Bundle[] bundles = context.getBundles();
		BundleInfo[] bInfos = new BundleInfo[bundles.length];
		for (int i = 0; i < bundles.length; i++) {
			//			System.out.println("bundles[" + i + "]=" + bundles[i]);
			if (bundles[i].getBundleId() == 0) // SystemBundle
				bInfos[i] = new BundleInfo(fwJarLocation, startLevel.getBundleStartLevel(bundles[i]), startLevel.isBundlePersistentlyStarted(bundles[i]), bundles[i].getBundleId());
			else
				bInfos[i] = new BundleInfo(FileUtils.getRealLocation(this, bundles[i].getLocation(), true), startLevel.getBundleStartLevel(bundles[i]), startLevel.isBundlePersistentlyStarted(bundles[i]), bundles[i].getBundleId());
		}
		configData.setBundles(bInfos);
		platformProperties = this.getRunningPlatformProperties();

		// copy system properties to ConfigData
		Properties props = System.getProperties();
		for (Enumeration enumeration = props.keys(); enumeration.hasMoreElements();) {
			String key = (String) enumeration.nextElement();
			String value = props.getProperty(key);
			if (toBeEliminated(key))
				continue;
			if (EquinoxFwConfigFileParser.isFwDependent(key))
				configData.setFwDependentProp(key, value);
			else
				configData.setFwIndependentProp(key, value);
		}

		// update initialBundleStartLevel
		BundleHelper helper = BundleHelper.getDefault();//getBundleHelper();
		StartLevel slAdmin = (StartLevel) helper.acquireService(StartLevel.class.getName());

		int initialBSL = configData.getInitialBundleStartLevel();
		if (initialBSL != slAdmin.getInitialBundleStartLevel())
			configData.setInitialBundleStartLevel(slAdmin.getInitialBundleStartLevel());

		//		for (int j = 0; j < bInfos.length; j++)
		//			configData.addBundle(bInfos[j]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.frameworkadmin.Manipulator#load()
	 */
	public void load() throws IllegalStateException, IOException, FrameworkAdminRuntimeException {
		Log.log(LogService.LOG_DEBUG, this, "load()", "BEGIN");
		loadWithoutFwPersistentData();

		BundlesState bundlesState = null;
		if (EquinoxBundlesState.checkFullySupported()) {
			//	bundlesState = new EquinoxBundlesState(context, fwAdmin, this, true, runtime);
			bundlesState = new EquinoxBundlesState(context, fwAdmin, this, !launcherData.isClean());
			platformProperties = ((EquinoxBundlesState) bundlesState).getPlatformProperties();

		} else {
			bundlesState = new SimpleBundlesState(fwAdmin, this, EquinoxConstants.FW_SYMBOLIC_NAME);
			platformProperties.clear();
		}
		updateAccordingToExpectedState(bundlesState);
		//		if (!useConfigurator)
		//			return;
		setConfiguratorManipulator();
		if (this.configuratorManipulator == null)
			return;
		configuratorManipulator.updateBundles(this);
		return;
	}

	private void loadWithoutFwPersistentData() throws IOException {
		SimpleBundlesState.checkAvailability(fwAdmin);
		File launcherConfigFile = getLauncherConfigLocation(launcherData);
		if (launcherConfigFile != null) {
			// use launcher. -- > load from LaucnherConfig file.
			// the parameters in memory will be updated.
			EclipseLauncherParser parser = new EclipseLauncherParser();
			parser.read(launcherData);
		}
		checkConsistencyOfFwConfigLocAndFwPersistentDataLoc(launcherData);

		File fwConfigFile = new File(launcherData.getFwConfigLocation(), EquinoxConstants.CONFIG_INI);
		EquinoxFwConfigFileParser parser = new EquinoxFwConfigFileParser(context);
		if (fwConfigFile.exists())
			parser.readFwConfig(this, fwConfigFile);

	}

	// Save all parameter in memory into proper config files.
	public void save(boolean backup) throws IOException, FrameworkAdminRuntimeException {
		Log.log(LogService.LOG_DEBUG, this, "save()", "BEGIN");
		SimpleBundlesState.checkAvailability(fwAdmin);

		try {
			updateAccordingToExpectedState(this.getBundlesState());
		} catch (IllegalStateException e) {
			// ignore.
		}
		//		File fwJar = EquinoxBundlesState.getFwJar(launcherData, configData);
		//		if (fwJar != null)
		//			launcherData.setFwJar(fwJar);

		File launcherConfigFile = getLauncherConfigLocation(launcherData);
		if (launcherConfigFile != null) {
			// Use launcher. -- > save LaucnherConfig file.
			EclipseLauncherParser launcherParser = new EclipseLauncherParser();
			launcherParser.save(launcherData, true, backup);
		}

		checkConsistencyOfFwConfigLocAndFwPersistentDataLoc(launcherData);

		//if (context != null)
		setConfiguratorManipulator();

		BundleInfo[] newBInfos = null;
		if (configuratorManipulator != null) { // Optimize BundleInfo[] 
			try {
				newBInfos = configuratorManipulator.save(this, backup);
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				if (LOG_ILLEGALSTATEEXCEPTION)
					Log.log(LogService.LOG_WARNING, this, "save()", e);
				newBInfos = configData.getBundles();
			}
		} else
			newBInfos = configData.getBundles();
		// Save FwConfigFile
		EquinoxFwConfigFileParser parser = new EquinoxFwConfigFileParser(context);
		parser.saveFwConfig(newBInfos, this, backup, false);
	}

	public void setConfigData(ConfigData configData) {
		this.configData.initialize();
		this.configData.setInitialBundleStartLevel(configData.getInitialBundleStartLevel());
		this.configData.setBeginningFwStartLevel(configData.getBeginingFwStartLevel());
		BundleInfo[] bInfos = configData.getBundles();
		for (int i = 0; i < bInfos.length; i++)
			this.configData.addBundle(bInfos[i]);
		this.configData.setFwIndependentProps(configData.getFwIndependentProps());
		if (this.configData.getFwName().equals(configData.getFwName()))
			if (this.configData.getFwVersion().equals(configData.getFwVersion())) {
				// TODO refine the algorithm to copying fw dependent props.
				//  configData.getFwName()/getFwVersion()/
				//	getLauncherName()/getLauncherVersion() might be taken into consideration. 
				this.configData.setFwDependentProps(configData.getFwDependentProps());
			}
	}

	/**
	 * 1. get all ServiceReferences of ConfiguratorManipulator.   
	 * 2. Check if there any ConfiguratorBundle in the Bundles list that can be manipulated by 
	 * 	the available ConfiguratorManipulators.
	 * 3. Choose the one that will be firstly started among them.
	 * 4. set the object that corresponds to the chosen ConfiguratorBundle.  
	 * 
	 */
	private void setConfiguratorManipulator() {
		if (context == null) {
			this.configuratorManipulator = this.fwAdmin.getConfiguratorManipulator();
			return;
		}
		ServiceReference[] references = cmTracker.getServiceReferences();

		int count = cmTracker.getTrackingCount();
		if (count == this.trackingCount)
			return;
		this.trackingCount = count;

		BundleInfo[] bInfos = configData.getBundles();
		int initialBSL = configData.getInitialBundleStartLevel();
		bInfos = Utils.sortBundleInfos(bInfos, initialBSL);
		//int index = -1;	
		configuratorManipulator = null;
		if (references == null)
			return;
		for (int i = 0; i < bInfos.length; i++) {
			String location = bInfos[i].getLocation();
			location = FileUtils.getRealLocation(this, location, true);
			if (!bInfos[i].isMarkedAsStarted())
				continue;
			for (int j = 0; j < references.length; j++)
				if (references[j].getProperty(ConfiguratorManipulator.SERVICE_PROP_KEY_CONFIGURATOR_BUNDLESYMBOLICNAME).equals(Utils.getManifestMainAttributes(location, Constants.BUNDLE_SYMBOLICNAME))) {
					configuratorManipulator = (ConfiguratorManipulator) cmTracker.getService(references[j]);
					break;
				}
			if (configuratorManipulator != null)
				break;
		}
	}

	public void setLauncherData(LauncherData launcherData) {
		this.launcherData.initialize();
		this.launcherData.setFwConfigLocation(launcherData.getFwConfigLocation());
		this.launcherData.setFwPersistentDataLocation(launcherData.getFwPersistentDataLocation(), launcherData.isClean());
		this.launcherData.setJvm(launcherData.getJvm());
		this.launcherData.setJvmArgs(launcherData.getJvmArgs());
		if (this.launcherData.getFwName().equals(launcherData.getFwName()))
			if (this.launcherData.getFwVersion().equals(launcherData.getFwVersion())) {
				// TODO launcherData.getFwName()/getFwVersion()/
				//	getLauncherName()/getLauncherVersion() might be taken into consideration
				//  for copying . 
				this.launcherData.setFwJar(launcherData.getFwJar());
				this.launcherData.setHome(launcherData.getHome());
				this.launcherData.setLauncher(launcherData.getLauncher());
				this.launcherData.setLauncherConfigLocation(launcherData.getLauncherConfigLocation());
			}
	}

	/**
	 * Temporal implementation.
	 * 
	 * If a property of the given key should be eliminated
	 *  from FwDependentProperties and FwIndependentProperties,
	 *  return true. Otherwise false.
	 * 
	 * @param key
	 * @return true if it should be elimineted from FwDependentProperties and FwIndependentProperties,
	 */
	private boolean toBeEliminated(String key) {
		if (key.startsWith("java."))
			return true;
		return false;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("++++++++++++++++++++++++++++++++++++++++++\n" + "Class:" + this.getClass().getName() + "\n");
		sb.append("------------- LauncherData -----------\n");
		sb.append(launcherData.toString());
		sb.append("------------- ConfigData -----------\n");
		sb.append(configData.toString());
		sb.append("\n" + Utils.toStringProperties("platformProperties", this.platformProperties));
		sb.append("++++++++++++++++++++++++++++++++++++++++++\n");
		return sb.toString();
	}

	private void updateAccordingToExpectedState(BundlesState bundlesState) {
		File newFwJar = EquinoxBundlesState.getFwJar(launcherData, configData);
		if (bundlesState instanceof EquinoxBundlesState)
			((EquinoxBundlesState) bundlesState).setFwJar(newFwJar);

		if (launcherData.getFwJar() == null && newFwJar != null)
			launcherData.setFwJar(newFwJar);
		BundleInfo[] newBundleInfos = bundlesState.getExpectedState();
		configData.setBundles(newBundleInfos);
	}
}
