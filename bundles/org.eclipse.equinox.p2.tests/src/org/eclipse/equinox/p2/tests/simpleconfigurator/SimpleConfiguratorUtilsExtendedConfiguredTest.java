/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc. - fragment support
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.simpleconfigurator;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import org.eclipse.equinox.internal.simpleconfigurator.Activator;
import org.eclipse.equinox.internal.simpleconfigurator.utils.BundleInfo;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorUtils;
import org.eclipse.equinox.p2.tests.sharedinstall.AbstractSharedInstallTest;

public class SimpleConfiguratorUtilsExtendedConfiguredTest extends SimpleConfiguratorUtilsExtendedTest {

	private File testData;
	private File mainBundlesInfo;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		testData = getTempFolder();
		copy("preparing readonly data", getTestData("simpleconfigurator extensions", "testData/simpleConfiguratorExtendedTest"), testData);
		testData = new File(testData, "extensions");
		AbstractSharedInstallTest.setReadOnly(testData.getParentFile(), true);
		AbstractSharedInstallTest.reallyReadOnly(testData.getParentFile());
		Activator.EXTENSIONS = testData.toString();

		mainBundlesInfo = getTestData("simpleconfigurator extensions - main bundles.info", "testData/simpleConfiguratorExtendedTest/main/bundles.info");
	}

	@Override
	protected void tearDown() throws Exception {
		Activator.EXTENSIONS = null;
		AbstractSharedInstallTest.removeReallyReadOnly(testData.getParentFile());
		AbstractSharedInstallTest.setReadOnly(testData.getParentFile(), false);
		testData.getParentFile().delete();
		super.tearDown();
	}

	@SuppressWarnings("deprecation")
	public void testMultipleInfoFiles() throws MalformedURLException, IOException {
		List<BundleInfo> readConfiguration = SimpleConfiguratorUtils.readConfiguration(mainBundlesInfo.toURL(), mainBundlesInfo.getParentFile().toURI());
		BundleInfo a = getBundle("a", readConfiguration);
		assertNotNull("Bundle from the main list not loaded", a);
		assertEquals("Path not resolved properly for the main bundles.info", new File(mainBundlesInfo.getParentFile(), "plugins/a_1.0.0.jar").toURI(), getLocation(a));

		if (!AbstractSharedInstallTest.WINDOWS) {
			BundleInfo b = getBundle("b", readConfiguration);
			assertNotNull("Bundle from the main list not loaded", b);
			assertEquals("Path not resolved properly for the main bundles.info", new File("/b_1.0.0.jar").toURI(), getLocation(b));
		}

		//check false positive
		BundleInfo x = getBundle("x", readConfiguration);
		assertNull("This bundle is not listed anywhere!", x);

		BundleInfo g = getBundle("g", readConfiguration);
		assertNotNull("Bundle from the direct extension not loaded", g);
		assertEquals("Path not resolved properly from direct extension", new File(testData, "extension1/plugins/g_1.0.0.jar").toURI(), getLocation(g));

		if (!AbstractSharedInstallTest.WINDOWS) {
			BundleInfo h = getBundle("h", readConfiguration);
			assertNotNull("Bundle from the direct extension not loaded", h);
			assertEquals("Path not resolved properly from direct extension", new File("/h_1.0.0.jar").toURI(), getLocation(h));
		}

		BundleInfo m = getBundle("m", readConfiguration);
		assertNotNull("Bundle from the linked extension not loaded", m);
		assertEquals("Path not resolved properly from linked extension", new File(testData.getParentFile(), "extension2/m_1.0.0.jar").toURI(), getLocation(m));

		if (!AbstractSharedInstallTest.WINDOWS) {
			BundleInfo n = getBundle("n", readConfiguration);
			assertNotNull("Bundle from the linked extension not loaded", n);
			assertEquals("Path not resolved properly from linked extension", new File("/n_1.0.0.jar").toURI(), getLocation(n));
		}
	}

	@SuppressWarnings("deprecation")
	public void testMultipleLocations() throws MalformedURLException, IOException {
		Activator.EXTENSIONS = testData.toString() + "," + new File(testData.getParentFile(), "extensionsForReconciler1");

		List<BundleInfo> readConfiguration = SimpleConfiguratorUtils.readConfiguration(mainBundlesInfo.toURL(), mainBundlesInfo.getParentFile().toURI());
		BundleInfo a = getBundle("a", readConfiguration);
		assertNotNull("Bundle from the main list not loaded", a);
		assertEquals("Path not resolved properly for the main bundles.info", new File(mainBundlesInfo.getParentFile(), "plugins/a_1.0.0.jar").toURI(), getLocation(a));

		if (!AbstractSharedInstallTest.WINDOWS) { //test use linux absolute paths
			BundleInfo b = getBundle("b", readConfiguration);
			assertNotNull("Bundle from the main list not loaded", b);
			assertEquals("Path not resolved properly for the main bundles.info", new File("/b_1.0.0.jar").toURI(), getLocation(b));
		}

		//check false positive
		BundleInfo x = getBundle("x", readConfiguration);
		assertNull("This bundle is not listed anywhere!", x);

		BundleInfo g = getBundle("g", readConfiguration);
		assertNotNull("Bundle from the direct extension not loaded", g);
		assertEquals("Path not resolved properly from direct extension", new File(testData, "extension1/plugins/g_1.0.0.jar").toURI(), getLocation(g));

		if (!AbstractSharedInstallTest.WINDOWS) { //test use linux absolute paths
			BundleInfo h = getBundle("h", readConfiguration);
			assertNotNull("Bundle from the direct extension not loaded", h);
			assertEquals("Path not resolved properly from direct extension", new File("/h_1.0.0.jar").toURI(), getLocation(h));
		}

		BundleInfo m = getBundle("m", readConfiguration);
		assertNotNull("Bundle from the linked extension not loaded", m);
		assertEquals("Path not resolved properly from linked extension", new File(testData.getParentFile(), "extension2/m_1.0.0.jar").toURI(), getLocation(m));

		if (!AbstractSharedInstallTest.WINDOWS) { //test use linux absolute paths
			BundleInfo n = getBundle("n", readConfiguration);
			assertNotNull("Bundle from the linked extension not loaded", n);
			assertEquals("Path not resolved properly from linked extension", new File("/n_1.0.0.jar").toURI(), getLocation(n));
		}
	}

	//on adding extension master must be selected in order to create new profile with extensions!
	public void testSharedConfigurationMasterUnmodified() throws IOException {
		storeTimestamp(new File(masterConfguration, relativeURL.getFile()).lastModified());
		assertEquals(sharedConfiguration[1], configurator.chooseConfigurationURL(relativeURL, sharedConfiguration));
		assertIsPropertySet(true);
	}

	private BundleInfo getBundle(String name, List<BundleInfo> list) {
		for (BundleInfo info : list) {
			if (info.getSymbolicName().equals(name)) {
				return info;
			}
		}
		return null;
	}

	private URI getLocation(BundleInfo b) {
		if (b.getBaseLocation() != null) {
			return b.getBaseLocation().resolve(b.getLocation());
		}
		return b.getLocation();
	}
}
