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
import java.net.URI;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.eclipse.equinox.internal.simpleconfigurator.manipulator.SimpleConfiguratorManipulatorImpl;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SimpleConfiguratorManipulatorTests extends AbstractProvisioningTest {

	public void testSaveConfiguration() throws Exception {
		File folder = getTestFolder("saveConfiguration");
		File infoFile = new File(folder, "bundle.info");

		File baseFile = getTempFolder();

		//absolute location written with base
		BundleInfo[] bundles = new BundleInfo[] {new BundleInfo("a", "1.0.0", new File(folder, "plugins/a_1.0.0.jar").toURI(), BundleInfo.NO_LEVEL, false)};
		SimpleConfiguratorManipulator manipulator = new SimpleConfiguratorManipulatorImpl();
		manipulator.saveConfiguration(bundles, infoFile, folder);
		bundles = manipulator.loadConfiguration(infoFile.toURL(), baseFile);
		assertEquals(bundles[0].getLocation(), new File(baseFile, "plugins/a_1.0.0.jar").toURI());

		//relative location written with null base
		bundles = new BundleInfo[] {new BundleInfo("b", "1.0.0", new URI("plugins/b_1.0.0.jar"), BundleInfo.NO_LEVEL, false)};
		manipulator.saveConfiguration(bundles, infoFile, null);
		bundles = manipulator.loadConfiguration(infoFile.toURL(), baseFile);
		assertEquals(bundles[0].getLocation(), new File(baseFile, "plugins/b_1.0.0.jar").toURI());

		//absolute location written with null base
		URI absolute = new File(folder, "plugins/c_1.0.0.jar").toURI();
		bundles = new BundleInfo[] {new BundleInfo("c", "1.0.0", absolute, BundleInfo.NO_LEVEL, false)};
		manipulator.saveConfiguration(bundles, infoFile, null);
		bundles = manipulator.loadConfiguration(infoFile.toURL(), baseFile);
		assertEquals(bundles[0].getLocation(), absolute);
	}
}
