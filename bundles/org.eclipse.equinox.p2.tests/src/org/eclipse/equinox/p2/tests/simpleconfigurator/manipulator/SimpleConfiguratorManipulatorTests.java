/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.simpleconfigurator.manipulator.SimpleConfiguratorManipulatorImpl;
import org.eclipse.equinox.internal.simpleconfigurator.utils.URIUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
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
}
