/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import java.io.File;
import java.io.IOException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.frameworkadmin.equinox.EquinoxConstants;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.osgi.service.environment.Constants;
import org.osgi.framework.BundleException;

public class MacOSLikeSetup extends FwkAdminAndSimpleConfiguratorTest {

	public MacOSLikeSetup(String name) {
		super(name);
	}

	public void testMacOSSetup() throws FrameworkAdminRuntimeException, IOException, BundleException {
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = new File(Activator.getContext().getDataFile(getName()), "Eclipse.app/Contents/Eclipse");
		File configurationFolder = new File(installFolder, "configuration");
		File launcherFolder = new File(installFolder, "../MacOS/"); 
		File launcherName = new File(launcherFolder, "eclipse");

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configurationFolder);
		launcherData.setLauncher(launcherName);
		launcherData.setLauncherConfigLocation(new File(installFolder, "eclipse.ini"));
		launcherData.setOS(Constants.OS_MACOSX);

		//Setup the plugins as they should
		File osgiJar = new File(installFolder, "plugins/org.eclipse.osgi.jar");
		File scJar = new File(installFolder, "plugins/org.eclipse.equinox.simpleconfigurator.jar");
		File launcherJar = new File(installFolder, "plugins/org.eclipse.equinox.launcher.jar");
		copy("OSGi", new File(FileLocator.toFileURL(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar")).getPath()), osgiJar);
		copy("SC", new File(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar")).getPath()), scJar);
		copy("Startup", new File(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.launcher.jar")).getPath()), launcherJar);
		
		manipulator.getConfigData().addBundle(new BundleInfo(osgiJar.toURI()));
		manipulator.getConfigData().addBundle(new BundleInfo(scJar.toURI(), 1, true));
		manipulator.getConfigData().addBundle(new BundleInfo(launcherJar.toURI()));
		
		manipulator.getLauncherData().addProgramArg(EquinoxConstants.OPTION_STARTUP);
		manipulator.getLauncherData().addProgramArg(launcherJar.toURI().toString());
		
		manipulator.getLauncherData().setFwJar(osgiJar);
		
		try {
			manipulator.save(false);
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}
		File launcherIni = new File(installFolder, "eclipse.ini");
		assertNotContent(launcherIni, "-configuration");
		assertNotContent(launcherIni, "-install");
		assertContent(launcherIni, "-startup");
		assertContent(launcherIni, "../Eclipse/plugins/org.eclipse.equinox.launcher.jar");
		assertNotContent(launcherIni, MacOSLikeSetup.class.getName());
		assertNotContent(new File(configurationFolder, "config.ini"), MacOSLikeSetup.class.getName());
		assertTrue("bundles.info missing", new File(configurationFolder, "org.eclipse.equinox.simpleconfigurator/bundles.info").exists());
		
	}
	
	public void testMacWithoutStartupOrFw() throws Exception {
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = new File(Activator.getContext().getDataFile(getName()), "Eclipse.app/Contents/Eclipse");
		File configurationFolder = new File(installFolder, "configuration");
		File launcherFolder = new File(installFolder, "../MacOS/"); 
		File launcherName = new File(launcherFolder, "eclipse");

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configurationFolder);
		launcherData.setLauncher(launcherName);
		launcherData.setOS(Constants.OS_MACOSX);
		
		File osgiJar = new File(installFolder, "plugins/org.eclipse.osgi.jar");
		File scJar = new File(installFolder, "plugins/org.eclipse.equinox.simpleconfigurator.jar");
		File bundle = new File(installFolder, "plugins/bundle_1");
		copy("OSGi", new File(FileLocator.toFileURL(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar")).getPath()), osgiJar);
		copy("SC", new File(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar")).getPath()), scJar);
		copy("bundle", new File(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/bundle_1")).getPath()), bundle);
		
		manipulator.getConfigData().addBundle(new BundleInfo(osgiJar.toURI()));
		manipulator.getConfigData().addBundle(new BundleInfo(scJar.toURI(), 1, true));
		manipulator.getConfigData().addBundle(new BundleInfo(bundle.toURI()));
		manipulator.save(false);
		
		File launcherIni = new File(installFolder, "eclipse.ini");
		File bundleInfo = new File(configurationFolder, "org.eclipse.equinox.simpleconfigurator/bundles.info");
		assertFalse(launcherIni.exists());
		assertContent(bundleInfo, "file:plugins/bundle_1/");
	}
}
