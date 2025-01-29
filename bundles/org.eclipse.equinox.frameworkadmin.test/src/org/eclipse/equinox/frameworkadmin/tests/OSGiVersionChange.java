/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdminRuntimeException;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleException;

public class OSGiVersionChange extends FwkAdminAndSimpleConfiguratorTest {
	private Manipulator defaultManipulator = null;
	private final String workArea = OSGiVersionChange.class.getName();


	@Override @Before
	public void setUp() throws Exception {
		super.setUp();
		defaultManipulator = createMinimalConfiguration(workArea);
	}

	@Test
	public void testRemovalUsingSameManipulator() throws IllegalStateException, FrameworkAdminRuntimeException, IOException {
		BundleInfo[] infos = defaultManipulator.getConfigData().getBundles();
		BundleInfo osgi = null;
		for (BundleInfo info : infos) {
			if ("org.eclipse.osgi".equals(info.getSymbolicName())) {
				osgi = info;
				break;
			}
		}
		assertEquals(true, defaultManipulator.getConfigData().removeBundle(osgi));
		defaultManipulator.save(false);
		assertNotContent(getBundleTxt(), "org.eclipse.osgi");
		assertNotPropertyContains(getConfigIni(),"osgi.bundles", "org.eclipse.osgi");
	}
	@Test
	public void testRemovalUsingOtherManipulator() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException {
		Manipulator newManipulator = getNewManipulator(workArea);
		BundleInfo[] infos = newManipulator.getConfigData().getBundles();
		BundleInfo osgi = null;
		for (BundleInfo info : infos) {
			if ("org.eclipse.osgi".equals(info.getSymbolicName())) {
				osgi = info;
				break;
			}
		}
		newManipulator.getConfigData().removeBundle(osgi);
		newManipulator.save(false);
		assertNotContent(getBundleTxt(), "org.eclipse.osgi");
		assertNotPropertyContains(getConfigIni(),"osgi.bundles", "org.eclipse.osgi");
	}
	@Test
	public void testAdditionUsingOtherManipulator() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException {
		BundleInfo[] infos = defaultManipulator.getConfigData().getBundles();
		BundleInfo osgi = null;
		for (BundleInfo info : infos) {
			if ("org.eclipse.osgi".equals(info.getSymbolicName())) {
				osgi = info;
				break;
			}
		}
		assertEquals(true, defaultManipulator.getConfigData().removeBundle(osgi));
		defaultManipulator.save(false);

		Manipulator newManipulator = getNewManipulator(workArea);

		newManipulator.getConfigData().addBundle(osgi);
		newManipulator.save(false);
		assertContent(getBundleTxt(), "org.eclipse.osgi");
		assertNotPropertyContains(getConfigIni(),"osgi.bundles", "org.eclipse.osgi");
	}
	@Test
	public void testChangeVersion() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, URISyntaxException {
		BundleInfo[] infos = defaultManipulator.getConfigData().getBundles();
		BundleInfo osgi = null;
		for (BundleInfo info : infos) {
			if ("org.eclipse.osgi".equals(info.getSymbolicName())) {
				osgi = info;
				break;
			}
		}
		defaultManipulator.getConfigData().removeBundle(osgi);
		defaultManipulator.save(false);

		//These two constants describe the data file used in the test
		final String FILENAME = "org.eclipse.osgi_3.4.0.jar";
		final String VERSION = "3.4.0.v20071105";
		BundleInfo newOSGi = new BundleInfo("org.eclipse.osgi", "3.4.0.v20071105", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/" + FILENAME))), 0, true);
		defaultManipulator.getConfigData().addBundle(newOSGi);
		defaultManipulator.save(false);
		assertContent(getBundleTxt(), VERSION);
		assertContent(getConfigIni(), FILENAME);
	}
	@Test
	public void testReadConfigWithoutOSGi() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException {
		//First Create a configuration that does not contain OSGi
		BundleInfo[] infos = defaultManipulator.getConfigData().getBundles();
		BundleInfo osgi = null;
		for (BundleInfo info : infos) {
			if ("org.eclipse.osgi".equals(info.getSymbolicName())) {
				osgi = info;
				break;
			}
		}
		defaultManipulator.getConfigData().removeBundle(osgi);
		defaultManipulator.save(false);
		assertNotContent(getBundleTxt(), "org.eclipse.osgi");
		assertNotPropertyContains(getConfigIni(),"osgi.bundles", "org.eclipse.osgi");

		Manipulator newManipulator = getNewManipulator(workArea);
		assertEquals(1, newManipulator.getConfigData().getBundles().length);
	}
}
