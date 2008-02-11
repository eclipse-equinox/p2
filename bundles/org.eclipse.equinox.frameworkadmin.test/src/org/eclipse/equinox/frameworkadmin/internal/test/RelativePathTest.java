/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.frameworkadmin.internal.test;

import java.io.*;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;

public class RelativePathTest extends FwkAdminAndSimpleConfiguratorTest {
	public RelativePathTest(String name) throws Exception {
		super(name);
	}

	public void testRelativePaths() throws Exception {
		File installFolder = Activator.getContext().getDataFile(RelativePathTest.class.getName());

		//First we copy some jars into a well
		File osgiJar = new File(installFolder, "plugins/org.eclipse.osgi.jar");
		osgiJar.getParentFile().mkdirs();
		File scJar = new File(installFolder, "plugins/org.eclipse.equinox.simpleconfigurator.jar");

		copyStream(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar").openStream(), true, new FileOutputStream(osgiJar), true);
		copyStream(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar").openStream(), true, new FileOutputStream(scJar), true);

		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File configurationFolder = new File(installFolder, "configuration");
		String launcherName = "eclipse";

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherName));
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}

		BundleInfo osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1", osgiJar.toURL().toExternalForm(), 0, true);
		BundleInfo configuratorBi = new BundleInfo("org.eclipse.equinox.simpleconfigurator", "1.0.0", scJar.toURL().toExternalForm(), 1, true);
		manipulator.getConfigData().addBundle(osgiBi);
		manipulator.getConfigData().addBundle(configuratorBi);
		try {
			manipulator.save(false);
		} catch (IOException e) {
			fail("Error while persisting");
		} catch (FrameworkAdminRuntimeException e) {
			fail("Error while persisting");
		}

		//":path.jar" is a  poor man approach to test relative paths 
		assertNotContent(new File(configurationFolder, "config.ini"), installFolder.getAbsolutePath());
		assertNotContent(new File(configurationFolder, "org.eclipse.equinox.simpleconfigurator/bundles.txt"), installFolder.getAbsolutePath());
		assertContent(new File(configurationFolder, "config.ini"), ":org.eclipse.equinox.simpleconfigurator.jar");
		assertContent(new File(configurationFolder, "org.eclipse.equinox.simpleconfigurator/bundles.txt"), ":plugins\\org.eclipse.equinox.simpleconfigurator.jar");
		assertContent(new File(configurationFolder, "org.eclipse.equinox.simpleconfigurator/bundles.txt"), ":plugins\\org.eclipse.osgi.jar");

		BundleInfo bi = new BundleInfo(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/bundle_1")).toExternalForm(), 2);
		manipulator.getConfigData().addBundle(bi);
		manipulator.save(false);
//		assertContent(new File(configurationFolder, "org.eclipse.equinox.simpleconfigurator/bundles.txt"), FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/bundle_1")).toExternalForm());

		Manipulator newManipulator = fwkAdmin.getManipulator();
		LauncherData newLauncherData = newManipulator.getLauncherData();
		newLauncherData.setFwConfigLocation(configurationFolder);
		newLauncherData.setLauncher(new File(installFolder, launcherName));
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}

	}
}
