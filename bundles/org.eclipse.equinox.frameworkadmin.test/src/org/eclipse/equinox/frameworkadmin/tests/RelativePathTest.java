/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.frameworkadmin.equinox.EquinoxManipulatorImpl;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.junit.Test;

public class RelativePathTest extends FwkAdminAndSimpleConfiguratorTest {
	/** Constant value indicating if the current platform is Windows */
	private static final boolean WINDOWS = java.io.File.separatorChar == '\\';

	@Test
	public void testRelativePaths() throws Exception {
		File installFolder = Activator.getContext().getDataFile(RelativePathTest.class.getName());

		// First we copy some jars into a well
		File osgiJar = new File(installFolder, "plugins/org.eclipse.osgi.jar");
		osgiJar.getParentFile().mkdirs();
		File scJar = new File(installFolder, "plugins/org.eclipse.equinox.simpleconfigurator.jar");

		copyStream(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar").openStream(), true,
				new FileOutputStream(osgiJar), true);
		copyStream(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar")
				.openStream(), true, new FileOutputStream(scJar), true);

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
			// TODO We ignore the framework JAR location not set exception
		}

		BundleInfo osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1", osgiJar.toURI(), 0, true);
		BundleInfo configuratorBi = new BundleInfo("org.eclipse.equinox.simpleconfigurator", "1.0.0", scJar.toURI(), 1,
				true);
		manipulator.getConfigData().addBundle(osgiBi);
		manipulator.getConfigData().addBundle(configuratorBi);
		manipulator.save(false);

		// ":path.jar" is a poor man approach to test relative paths
		assertNotContent(new File(configurationFolder, "config.ini"), installFolder.getAbsolutePath());
		assertNotContent(new File(configurationFolder, "org.eclipse.equinox.simpleconfigurator/bundles.info"),
				installFolder.getAbsolutePath());
		assertContent(new File(configurationFolder, "config.ini"), ":org.eclipse.equinox.simpleconfigurator.jar");
		// Note: This is testing for old style bundle locations
		assertContent(new File(configurationFolder, "org.eclipse.equinox.simpleconfigurator/bundles.info"),
				"file:plugins/org.eclipse.equinox.simpleconfigurator.jar");
		assertContent(new File(configurationFolder, "org.eclipse.equinox.simpleconfigurator/bundles.info"),
				"file:plugins/org.eclipse.osgi.jar");

		BundleInfo bi = new BundleInfo(
				URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/bundle_1"))), 2,
				false);
		manipulator.getConfigData().addBundle(bi);
		manipulator.save(false);
		// assertContent(new File(configurationFolder,
		// "org.eclipse.equinox.simpleconfigurator/bundles.info"),
		// FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/bundle_1")).toExternalForm());

		Manipulator newManipulator = fwkAdmin.getManipulator();
		LauncherData newLauncherData = newManipulator.getLauncherData();
		newLauncherData.setFwConfigLocation(configurationFolder);
		newLauncherData.setLauncher(new File(installFolder, launcherName));
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			// TODO We ignore the framework JAR location not set exception
		}

	}

	@Test
	public void testMakeRelative_NonWindows() throws MalformedURLException {
		if (WINDOWS) {
			return;
		}
		URL base = new URL("file:/eclipse/");
		// data - [0] is the test data and [1] is the expected result
		String[][] data = new String[][] { //
				new String[] { "file:/home/eclipse/foo.jar", "file:../home/eclipse/foo.jar" }, //
				new String[] { "file:///home/eclipse/foo.jar", "file:../home/eclipse/foo.jar" }, //
		};
		for (int i = 0; i < data.length; i++) {
			assertEquals("1." + i, data[i][1], EquinoxManipulatorImpl.makeRelative(data[i][0], base));
		}
	}

	@Test
	public void testMakeRelative_Windows() throws MalformedURLException {
		if (!WINDOWS) {
			return;
		}
		// platform specific data
		URL base = new URL("file:/c:/a/eclipse/");
		// data - [0] is the test data and [1] is the expected result
		String[][] data = new String[][] {
				new String[] { "file:c:/b/shared/plugins/bar.jar", "file:../../b/shared/plugins/bar.jar" }, //
				new String[] { "file:d:/b/shared/plugins/bar.jar", "file:d:/b/shared/plugins/bar.jar" }, //
				new String[] { "file:/c:/a/eclipse/plugins/bar.jar", "file:plugins/bar.jar" }, //
				new String[] { "file:c:/a/eclipse/plugins/bar.jar", "file:plugins/bar.jar" }, //
				new String[] { "file:/c:/a/shared/plugins/bar.jar", "file:../shared/plugins/bar.jar" }, //
				new String[] { "file:/d:/a/eclipse/plugins/bar.jar", "file:/d:/a/eclipse/plugins/bar.jar" }, //
				new String[] { "file:/c:/x/eclipse/plugins/bar.jar", "file:../../x/eclipse/plugins/bar.jar" }, //
		};
		for (int i = 0; i < data.length; i++) {
			assertEquals("2." + i, data[i][1], EquinoxManipulatorImpl.makeRelative(data[i][0], base));
		}
	}
}
