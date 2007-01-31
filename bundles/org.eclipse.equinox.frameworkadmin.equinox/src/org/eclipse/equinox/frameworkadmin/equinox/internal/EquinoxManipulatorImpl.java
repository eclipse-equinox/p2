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
package org.eclipse.equinox.frameworkadmin.equinox.internal;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.equinox.configurator.ConfiguratorManipulator;
import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.internal.frameworkadmin.utils.SimpleBundlesState;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class EquinoxManipulatorImpl implements Manipulator {
	private static final String SYSTEMBUNDLE_SYMBOLICNAME = "org.eclipse.osg";
	public static final String FW_NAME = "Equinox";
	public static final String FW_VERSION = "3.3M5";
	public static final String LAUCNHER_NAME = "Eclipse.exe";
	public static final String LAUNCHER_VERSION = "3.2";

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
	Properties properties = new Properties();

	ServiceTracker cmTracker;
	int trackingCount = -1;

	ConfiguratorManipulator configuratorManipulator;

	EquinoxFwAdminImpl fwAdmin = null;

	EquinoxManipulatorImpl(BundleContext context, EquinoxFwAdminImpl fwAdmin) {
		this.context = context;
		this.fwAdmin = fwAdmin;
		cmTracker = new ServiceTracker(context, ConfiguratorManipulator.class.getName(), null);
		cmTracker.open();
	}

	public BundlesState getBundlesState() throws FrameworkAdminRuntimeException {
		if (!EquinoxBundlesState.checkFullySupported())
			return new SimpleBundlesState(context, fwAdmin, this, SYSTEMBUNDLE_SYMBOLICNAME);

		if (properties.isEmpty())
			return new EquinoxBundlesState(context, fwAdmin, this, false);
		// XXX checking if fwDependent or fwIndependent properties are updated after the properties was created might be required for better implementation.		
		return new EquinoxBundlesState(context, fwAdmin, this, properties);
	}

	public ConfigData getConfigData() throws FrameworkAdminRuntimeException {
		return configData;
	}

	public BundleInfo[] getExpectedState() throws IllegalArgumentException, IOException, FrameworkAdminRuntimeException {
		Log.log(LogService.LOG_DEBUG, this, "getExpectedState()", "BEGIN");
		SimpleBundlesState.checkAvailability(fwAdmin);

		BundlesState bundleState = this.getBundlesState();
		bundleState.resolve(true);
		return bundleState.getExpectedState();
	}

	public LauncherData getLauncherData() throws FrameworkAdminRuntimeException {
		return launcherData;
	}

	public void initialize() {
		Log.log(LogService.LOG_DEBUG, this, "initialize()", "BEGIN");
		configData.initialize();
		launcherData.initialize();
	}

	// Load parameters from LauncherConfigFile, FwConfigFile, and ConfiguratorConfigFile if required.
	// The parameter has been set will be updated.
	public void load() throws IOException, FrameworkAdminRuntimeException {
		Log.log(LogService.LOG_DEBUG, this, "load()", "BEGIN");
		this.load(true);
	}

	/**
	 * If useConfigurator is true, what a ConfiguratorBundle does at its startup will be taken into account.
	 * Otherwise, it won't. 
	 * 
	 * @param useConfigurator
	 * @throws IOException
	 * @throws FrameworkAdminRuntimeException
	 */
	public void load(boolean useConfigurator) throws IOException, FrameworkAdminRuntimeException {

		Log.log(LogService.LOG_DEBUG, this, "load()", "BEGIN");
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
		EquinoxFwConfigFileParser parser = new EquinoxFwConfigFileParser();
		if (fwConfigFile.exists())
			parser.readFwConfig(configData, fwConfigFile);

		BundlesState bundlesState = null;
		if (EquinoxBundlesState.checkFullySupported()) {
			bundlesState = new EquinoxBundlesState(context, fwAdmin, this, true);
			properties = ((EquinoxBundlesState) bundlesState).getProperties();
		} else {
			bundlesState = new SimpleBundlesState(context, fwAdmin, this, SYSTEMBUNDLE_SYMBOLICNAME);
			properties.clear();
		}
		BundleInfo[] newBundleInfos = bundlesState.getExpectedState();
		configData.setBundles(newBundleInfos);
		if (!useConfigurator)
			return;
		setConfiguratorManipulator();
		if (this.configuratorManipulator == null)
			return;
		configuratorManipulator.updateBundles(this);
		return;
	}

	// Save all parameter in memory into proper config files.
	public void save(boolean backup) throws IOException, FrameworkAdminRuntimeException {
		Log.log(LogService.LOG_DEBUG, this, "save()", "BEGIN");
		SimpleBundlesState.checkAvailability(fwAdmin);

		File launcherConfigFile = getLauncherConfigLocation(launcherData);
		if (launcherConfigFile != null) {
			// Use launcher. -- > save LaucnherConfig file.
			EclipseLauncherParser launcherParser = new EclipseLauncherParser();
			launcherParser.save(launcherData, true, backup);
		}

		checkConsistencyOfFwConfigLocAndFwPersistentDataLoc(launcherData);

		setConfiguratorManipulator();

		BundleInfo[] newBInfo = null;
		if (configuratorManipulator != null) // Optimize BundleInfo[] 
			newBInfo = configuratorManipulator.save(this, backup);
		else
			newBInfo = configData.getBundles();
		// Save FwConfigFile
		EquinoxFwConfigFileParser parser = new EquinoxFwConfigFileParser();
		parser.saveFwConfig(newBInfo, this, backup, false);
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
		for (int i = 0; i < bInfos.length; i++) {
			//String location = bInfos[i].getLocation();
			if (!bInfos[i].isMarkedAsStarted())
				continue;
			for (int j = 0; j < references.length; j++)
				if (references[j].getProperty(ConfiguratorManipulator.SERVICE_PROP_KEY_CONFIGURATOR_BUNDLESYMBOLICNAME).equals(Utils.getManifestMainAttributes(bInfos[i].getLocation(), Constants.BUNDLE_SYMBOLICNAME))) {
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
				this.launcherData.setLauncher(launcherData.getLauncher());
				this.launcherData.setLauncherConfigLocation(launcherData.getLauncherConfigLocation());
			}
	}

}
