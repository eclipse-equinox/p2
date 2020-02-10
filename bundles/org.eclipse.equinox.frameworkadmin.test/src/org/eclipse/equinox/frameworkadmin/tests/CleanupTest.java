/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdminRuntimeException;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.junit.Before;
import org.junit.Test;

public class CleanupTest extends FwkAdminAndSimpleConfiguratorTest {

	Manipulator m = null;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		m = createMinimalConfiguration(CleanupTest.class.getName());
	}

	@Test
	public void testSimpleConfiguratorRemoval() throws FrameworkAdminRuntimeException, IOException {
		BundleInfo[] bis = m.getConfigData().getBundles();
		for (BundleInfo bi : bis) {
			if (bi.getSymbolicName().equals("org.eclipse.equinox.simpleconfigurator")) {
				m.getConfigData().removeBundle(bi);
			}
		}
		m.save(false);
		assertFalse(new File(getConfigurationFolder(), "org.eclipse.equinox.simpleconfigurator").exists());
		assertTrue(getConfigurationFolder().exists());
		assertTrue(getConfigurationFolder().isDirectory());

		// Now remove osgi
		bis = m.getConfigData().getBundles();
		for (BundleInfo bi : bis) {
			if (bi.getSymbolicName().equals("org.eclipse.osgi")) {
				m.getConfigData().removeBundle(bi);
			}
		}
		m.save(false);
		assertFalse(getConfigurationFolder().exists());
		assertFalse(new File(getInstallFolder(), getLauncherName() + ".ini").exists());
	}

	@Test
	public void testOSGiRemoval() throws FrameworkAdminRuntimeException, IOException {
		BundleInfo[] bis = m.getConfigData().getBundles();
		for (BundleInfo bi : bis) {
			if (bi.getSymbolicName().equals("org.eclipse.osgi")) {
				m.getConfigData().removeBundle(bi);
			}
		}
		m.save(false);
		File confFile = new File(getConfigurationFolder(), "org.eclipse.equinox.simpleconfigurator");
		assertTrue(confFile.exists());
		assertTrue(confFile.isDirectory());
		assertTrue(getConfigurationFolder().exists());
		assertTrue(getConfigurationFolder().isDirectory());
		assertNotContent(new File(getConfigurationFolder(), "org.eclipse.equinox.simpleconfigurator/bundles.info"),
				"org.eclipse.osgi");

		bis = m.getConfigData().getBundles();
		for (BundleInfo bi : bis) {
			if (bi.getSymbolicName().equals("org.eclipse.equinox.simpleconfigurator")) {
				m.getConfigData().removeBundle(bi);
			}
		}
		m.save(false);
		assertFalse(getConfigurationFolder().exists());
		assertFalse(new File(getInstallFolder(), getLauncherName() + ".ini").exists());
	}

	@Test
	public void testWithMutipleBundles() throws IOException, URISyntaxException {
		BundleInfo bi = new BundleInfo(
				URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/bundle_1"))), 2,
				false);
		m.getConfigData().addBundle(bi);
		m.save(false);

		BundleInfo[] bis = m.getConfigData().getBundles();
		for (BundleInfo bi1 : bis) {
			if (bi1.getSymbolicName().equals("org.eclipse.equinox.simpleconfigurator")) {
				m.getConfigData().removeBundle(bi1);
			}
		}
		m.save(false);

		assertFalse(getBundleTxt().exists());
		assertContent(getConfigIni(), "bundle_1");
	}
}
