/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import java.io.File;
import java.io.IOException;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.BundleException;

public class SharedConfigurationTest extends AbstractFwkAdminTest {

	public SharedConfigurationTest(String name) {
		super(name);
	}
	
	public void testDefaultConfiguration() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException {
		startSimpleConfiguratorManipulator();
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = Activator.getContext().getDataFile(SharedConfigurationTest.class.getName());
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

		File installFolder = Activator.getContext().getDataFile(SharedConfigurationTest.class.getName());
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

		File installFolder = Activator.getContext().getDataFile(SharedConfigurationTest.class.getName());
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
}
