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

package org.eclipse.equinox.p2.tests.simpleconfigurator.manipulator;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.simpleconfigurator.Activator;
import org.eclipse.equinox.internal.simpleconfigurator.manipulator.SimpleConfiguratorManipulatorImpl;
import org.eclipse.equinox.internal.simpleconfigurator.utils.EquinoxUtils;
import org.eclipse.equinox.internal.simpleconfigurator.utils.URIUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.reconciler.dropins.SharedInstallTests;
import org.eclipse.equinox.p2.tests.sharedinstall.AbstractSharedInstallTest;
import org.eclipse.equinox.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;

public class SimpleConfiguratorManipulatorTests extends AbstractProvisioningTest {

	public void testSaveConfiguration() throws Exception {
		File folder = getTestFolder("saveConfiguration");
		File infoFile = new File(folder, "bundle.info");

		URI baseFile = getTempFolder().toURI();

		//absolute location written with base
		BundleInfo[] bundles = new BundleInfo[] {new BundleInfo("a", "1.0.0", new File(folder, "plugins/a_1.0.0.jar").toURI(), BundleInfo.NO_LEVEL, false)};
		SimpleConfiguratorManipulator manipulator = new SimpleConfiguratorManipulatorImpl();
		manipulator.saveConfiguration(bundles, infoFile, folder.toURI());
		bundles = manipulator.loadConfiguration(new FileInputStream(infoFile), baseFile);
		assertEquals(bundles[0].getLocation(), URIUtil.append(baseFile, "plugins/a_1.0.0.jar"));

		//relative location written with null base
		bundles = new BundleInfo[] {new BundleInfo("b", "1.0.0", new URI("plugins/b_1.0.0.jar"), BundleInfo.NO_LEVEL, false)};
		manipulator.saveConfiguration(bundles, infoFile, null);
		bundles = manipulator.loadConfiguration(new FileInputStream(infoFile), baseFile);
		assertEquals(bundles[0].getLocation(), URIUtil.append(baseFile, "plugins/b_1.0.0.jar"));

		//absolute location written with null base
		URI absolute = new File(folder, "plugins/c_1.0.0.jar").toURI();
		bundles = new BundleInfo[] {new BundleInfo("c", "1.0.0", absolute, BundleInfo.NO_LEVEL, false)};
		manipulator.saveConfiguration(bundles, infoFile, null);
		bundles = manipulator.loadConfiguration(new FileInputStream(infoFile), baseFile);
		assertEquals(bundles[0].getLocation(), absolute);
	}

	public void testLocationEncoding() throws Exception {
		File folder = getTestFolder("locationEncoding");
		File configurationFile = new File(folder, "bundle.info");

		BundleInfo[] bundles = new BundleInfo[2];
		bundles[0] = new BundleInfo("a", "1.0.0", new File(folder, "plu%2Cins/a_1.0.0.jar").toURI(), BundleInfo.NO_LEVEL, false);
		bundles[1] = new BundleInfo("b", "1.0.0", new File(folder, "plu,ins/b_1.0.0.jar").toURI(), BundleInfo.NO_LEVEL, false);

		SimpleConfiguratorManipulator manipulator = new SimpleConfiguratorManipulatorImpl();
		manipulator.saveConfiguration(bundles, configurationFile, folder.toURI());

		bundles = manipulator.loadConfiguration(new FileInputStream(configurationFile), folder.toURI());
		assertEquals(bundles[0].getLocation(), new File(folder, "plu%2Cins/a_1.0.0.jar").toURI());
		assertEquals(bundles[1].getLocation(), new File(folder, "plu,ins/b_1.0.0.jar").toURI());
	}

	public void testUTF8Encoding() throws Exception {
		File folder = getTestFolder("utf8Test");

		File configurationFile = new File(folder, "bundle.info");

		BundleInfo[] bundles = new BundleInfo[1];
		bundles[0] = new BundleInfo("a", "1.0.0", new File(folder, "\u0CA0_\u0CA0.jar").toURI(), BundleInfo.NO_LEVEL, false);

		SimpleConfiguratorManipulator manipulator = new SimpleConfiguratorManipulatorImpl();
		manipulator.saveConfiguration(bundles, configurationFile, folder.toURI());

		bundles = manipulator.loadConfiguration(new FileInputStream(configurationFile), folder.toURI());
		assertEquals(bundles[0].getLocation(), new File(folder, "\u0CA0_\u0CA0.jar").toURI());
	}

	public void testLoadConfigurationExtended() throws Exception {
		// See org.eclipse.equinox.p2.tests.simpleconfigurator.SimpleConfiguratorTests
		if (AbstractSharedInstallTest.WINDOWS) {
			return;
		}

		// installation info
		URI installArea = EquinoxUtils.getInstallLocationURI(TestActivator.getContext());

		// test info configured through p2.fragments
		File mainTestData = getTestData("0.0", "testData/simpleConfiguratorExtendedTest/main/bundles.info");
		URL configURL = EquinoxUtils.getConfigLocation(TestActivator.getContext()).getDataArea(SimpleConfiguratorManipulator.BUNDLES_INFO_PATH);
		File target = new File(configURL.getPath());
		target.getParentFile().mkdirs();
		target.createNewFile();
		copy("Copying ..", mainTestData, target);

		File fragTestData = getTestData("0.1", "/testData/simpleConfiguratorExtendedTest");
		File fragDir = getTempFolder();
		copy("Copying ..", fragTestData, fragDir);
		SharedInstallTests.setReadOnly(fragDir, true);
		Activator.EXTENDED = true;
		Activator.EXTENSIONS = fragDir.getAbsolutePath();

		List<String> expected = Arrays.asList(new String[] {"m,1.0.0", "n,1.0.0", "a,1.0.0", "b,1.0.0"});

		SimpleConfiguratorManipulator manipulator = new SimpleConfiguratorManipulatorImpl();
		BundleInfo[] installedInfo = manipulator.loadConfiguration(configURL.openStream(), installArea);
		BundleInfo[] installedAndExtendedInfo = manipulator.loadConfiguration(TestActivator.getContext(), SimpleConfiguratorManipulator.BUNDLES_INFO_PATH);

		List<BundleInfo> installedAndExtendedL = Arrays.asList(installedAndExtendedInfo);
		List<BundleInfo> installedL = Arrays.asList(installedInfo);
		List<BundleInfo> extendedL = new ArrayList<BundleInfo>(installedAndExtendedL);
		extendedL.removeAll(installedL);

		assertTrue(installedAndExtendedL.containsAll(installedL));
		assertFalse(extendedL.isEmpty());

		for (BundleInfo b : extendedL) {
			String actual = b.getSymbolicName() + "," + b.getVersion();
			if (!expected.contains(actual)) {
				fail(actual + " Could not be found in the list of expected bundle info entries.");
			}
		}

		SharedInstallTests.setReadOnly(fragDir, false);
	}
}
