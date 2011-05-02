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
package org.eclipse.equinox.frameworkadmin.tests;

import java.io.File;
import java.io.IOException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.BundleException;

public class RemovingABundle extends FwkAdminAndSimpleConfiguratorTest {

	public RemovingABundle(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		Manipulator manipulator = createMinimalConfiguration(RemovingABundle.class.getName());
		manipulator.getConfigData().addBundle(new BundleInfo("bundle_1", "1.0.0", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/bundle_1"))), 4, false));
		manipulator.save(false);

		File fooINI = new File(getInstallFolder(), getLauncherName() +".ini");
		assertEquals(fooINI.exists(), true);
		assertContent(getBundleTxt(), "bundle_1");
	}
	
	public void testRemoveBundleWithoutURL() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException {
		Manipulator m2 = getEquinoxFrameworkAdmin().getManipulator();

		LauncherData launcherData2 = m2.getLauncherData();
		launcherData2.setFwConfigLocation(getConfigurationFolder());
		launcherData2.setLauncher(new File(getInstallFolder(), "eclipse"));

		try {
			m2.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}
		BundleInfo info = new BundleInfo("bundle_1", "1.0.0", null, 0, false);
		m2.getConfigData().removeBundle(info);
		m2.save(false);

		assertNotContent(getBundleTxt(), "bundle_1");
	}
}
