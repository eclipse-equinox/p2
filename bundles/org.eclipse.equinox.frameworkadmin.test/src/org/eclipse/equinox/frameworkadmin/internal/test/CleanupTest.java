/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.frameworkadmin.internal.test;

import java.io.File;
import java.io.IOException;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.frameworkadmin.Manipulator;

public class CleanupTest extends FwkAdminAndSimpleConfiguratorTest {

	public CleanupTest(String name) {
		super(name);
	}

	Manipulator m = null;

	protected void setUp() throws Exception {
		super.setUp();
		m = createMinimalConfiguration(CleanupTest.class.getSimpleName());
	}

	public void testSimpleConfiguratorRemoval() {
		BundleInfo[] bis = m.getConfigData().getBundles();
		for (int i = 0; i < bis.length; i++) {
			if (bis[i].getSymbolicName().equals("org.eclipse.equinox.simpleconfigurator"))
				m.getConfigData().removeBundle(bis[i]);
		}
		try {
			m.save(false);
		} catch (IOException e) {
			fail("Error while saving");
		}
		assertNothing(new File(getConfigurationFolder(), "org.eclipse.equinox.simpleconfigurator"));
		assertIsDirectory(getConfigurationFolder());

		//Now remove osgi
		bis = m.getConfigData().getBundles();
		for (int i = 0; i < bis.length; i++) {
			if (bis[i].getSymbolicName().equals("org.eclipse.osgi"))
				m.getConfigData().removeBundle(bis[i]);
		}
		try {
			m.save(false);
		} catch (IOException e) {
			fail("Error while saving");
		}
		assertNothing(getConfigurationFolder());
		assertNothing(new File(getInstallFolder(), getLauncherName() + ".ini"));
	}

	public void testOSGiRemoval() {
		BundleInfo[] bis = m.getConfigData().getBundles();
		for (int i = 0; i < bis.length; i++) {
			if (bis[i].getSymbolicName().equals("org.eclipse.osgi"))
				m.getConfigData().removeBundle(bis[i]);
		}
		try {
			m.save(false);
		} catch (IOException e) {
			fail("Error while saving");
		}
		assertIsDirectory(new File(getConfigurationFolder(), "org.eclipse.equinox.simpleconfigurator"));
		assertIsDirectory(getConfigurationFolder());
		assertNotContent(new File(getConfigurationFolder(), "org.eclipse.equinox.simpleconfigurator/bundles.txt"), "org.eclipse.osgi");

		bis = m.getConfigData().getBundles();
		for (int i = 0; i < bis.length; i++) {
			if (bis[i].getSymbolicName().equals("org.eclipse.equinox.simpleconfigurator"))
				m.getConfigData().removeBundle(bis[i]);
		}
		try {
			m.save(false);
		} catch (IOException e) {
			fail("Error while saving");
		}
		assertNothing(getConfigurationFolder());
		assertNothing(new File(getInstallFolder(), getLauncherName() + ".ini"));
	}
}
