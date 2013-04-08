/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB (Pascal Rapicault) - Improve shared install
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Properties;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.frameworkadmin.equinox.Log;
import org.eclipse.equinox.internal.frameworkadmin.equinox.Messages;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogService;

public class SharedConfigurationTest extends AbstractFwkAdminTest {

	private static final String BASE_CONFIG_INI_TIMESTAMP = ".baseConfigIniTimestamp";

	public SharedConfigurationTest(String name) {
		super(name);
	}

	public void testDefaultConfiguration() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException {
		startSimpleConfiguratorManipulator();
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = Activator.getContext().getDataFile(getName());
		File defaultConfigurationFolder = new File(installFolder, "configuration");
		defaultConfigurationFolder.mkdirs();
		copy("creating shared config.ini", getTestData("", "dataFile/sharedconfiguration/config.ini"), new File(defaultConfigurationFolder, "config.ini"));

		String launcherName = "foo";

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(defaultConfigurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherName));

		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}

		assertEquals("false", manipulator.getConfigData().getProperty("config.shared"));
		assertEquals("true", manipulator.getConfigData().getProperty("from.parent"));
	}

	public void testSharedConfiguration() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException {
		startSimpleConfiguratorManipulator();
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = Activator.getContext().getDataFile(getName());
		File defaultConfigurationFolder = new File(installFolder, "configuration");
		defaultConfigurationFolder.mkdirs();
		copy("creating shared config.ini", getTestData("", "dataFile/sharedconfiguration/config.ini"), new File(defaultConfigurationFolder, "config.ini"));
		File userConfigurationFolder = new File(installFolder, "user/configuration");
		userConfigurationFolder.mkdirs();
		copy("creating shared config.ini", getTestData("", "dataFile/sharedconfiguration/user-config.ini"), new File(userConfigurationFolder, "config.ini"));

		String launcherName = "foo";

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(userConfigurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherName));

		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}

		assertEquals("true", manipulator.getConfigData().getProperty("config.shared"));
		assertEquals("true", manipulator.getConfigData().getProperty("from.parent"));
	}

	public void testNotSharedConfiguration() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException {
		startSimpleConfiguratorManipulator();
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = Activator.getContext().getDataFile(getName());
		File defaultConfigurationFolder = new File(installFolder, "configuration");
		defaultConfigurationFolder.mkdirs();
		copy("creating shared config.ini", getTestData("", "dataFile/sharedconfiguration/config.ini"), new File(defaultConfigurationFolder, "config.ini"));
		File userConfigurationFolder = new File(installFolder, "user/configuration");
		userConfigurationFolder.mkdirs();
		copy("creating shared config.ini", getTestData("", "dataFile/sharedconfiguration/user-noshare-config.ini"), new File(userConfigurationFolder, "config.ini"));

		String launcherName = "foo";

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(userConfigurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherName));

		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}

		assertEquals("false", manipulator.getConfigData().getProperty("config.shared"));
		assertEquals(null, manipulator.getConfigData().getProperty("from.parent"));
	}

	public void testConfigIniTimestamp() throws BundleException, FrameworkAdminRuntimeException, IOException, URISyntaxException {
		startSimpleConfiguratorManipulator();
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = Activator.getContext().getDataFile(getName());
		File defaultConfigurationFolder = new File(installFolder, "configuration");
		defaultConfigurationFolder.mkdirs();
		copy("creating shared config.ini", getTestData("", "dataFile/sharedconfiguration/config.ini"), new File(defaultConfigurationFolder, "config.ini"));
		File userConfigurationFolder = new File(installFolder, "user/configuration");
		userConfigurationFolder.mkdirs();
		copy("creating shared config.ini", getTestData("", "dataFile/sharedconfiguration/user-config.ini"), new File(userConfigurationFolder, "config.ini"));

		String launcherName = "foo";

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(userConfigurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherName));

		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			//TODO We ignore the framework JAR location not set exception
		}

		BundleInfo osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar"))), 0, true);
		BundleInfo configuratorBi = new BundleInfo("org.eclipse.equinox.simpleconfigurator", "1.0.0", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar"))), 1, true);

		manipulator.getConfigData().addBundle(osgiBi);
		manipulator.getConfigData().addBundle(configuratorBi);

		manipulator.save(false);
		File baseTimestamp = new File(userConfigurationFolder, BASE_CONFIG_INI_TIMESTAMP);
		assertIsFile(baseTimestamp);
		assertContent(baseTimestamp, Long.toString(new File(defaultConfigurationFolder, "config.ini").lastModified()));
	}

	public void testConfigurationIgnoredWhenChanged() throws BundleException, FrameworkAdminRuntimeException, IOException {
		startSimpleConfiguratorManipulator();
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		//setup the files
		File installFolder = Activator.getContext().getDataFile(getName());
		File defaultConfigurationFolder = new File(installFolder, "configuration");
		defaultConfigurationFolder.mkdirs();
		copy("creating shared config.ini", getTestData("", "dataFile/sharedconfiguration/ignoreUserConfig/config.ini"), new File(defaultConfigurationFolder, "config.ini"));
		File userConfigurationFolder = new File(installFolder, "user/configuration");
		userConfigurationFolder.mkdirs();
		copy("creating shared config.ini", getTestData("", "dataFile/sharedconfiguration/ignoreUserConfig/user-config.ini"), new File(userConfigurationFolder, "config.ini"));
		
		//setup the timestamp 
		Properties p = new Properties();
		p.setProperty("configIniTimestamp", Long.toString(new File(defaultConfigurationFolder, "config.ini").lastModified() - 10)); //Here we write an outdated timestamp to mimic the fact that the base has changed 
		saveProperties(new File(userConfigurationFolder, BASE_CONFIG_INI_TIMESTAMP), p);

		String launcherName = "foo";

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(userConfigurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherName));

		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}

		assertNull(manipulator.getConfigData().getProperty("userKey"));
		assertEquals("sharedValue", manipulator.getConfigData().getProperty("sharedKey"));
	}

	private void saveProperties(File outputFile, Properties configProps) throws IOException {
		String header = "This configuration file was written by: " + this.getClass().getName(); //$NON-NLS-1$
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(outputFile);
			configProps.store(out, header);
			Log.log(LogService.LOG_INFO, NLS.bind(Messages.log_propertiesSaved, outputFile));
		} finally {
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
