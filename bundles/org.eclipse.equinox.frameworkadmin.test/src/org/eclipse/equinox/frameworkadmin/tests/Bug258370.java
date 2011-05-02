/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.BundleException;

public class Bug258370 extends FwkAdminAndSimpleConfiguratorTest {
	public Bug258370(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	public void testComma() {
		FrameworkAdmin fwkAdmin = null;
		try {
			fwkAdmin = getEquinoxFrameworkAdmin();
		} catch (BundleException e1) {
			fail("0.0");
		}
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = Activator.getContext().getDataFile(SimpleConfiguratorTest.class.getName());
		File configurationFolder = new File(installFolder, "configuration");
		String launcherName = "eclipse";

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherName));
		try {
			try {
				manipulator.load();
			} catch (FrameworkAdminRuntimeException e) {
				fail("1.0");
			} catch (IOException e) {
				fail("2.0");
			}
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}

		BundleInfo osgiBi = null;
		BundleInfo bundle1Bi = null;
		BundleInfo bundle2Bi = null;

		try {
			osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar"))), 0, true);
			bundle1Bi = new BundleInfo("bundle_1", "1.0.0", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/bundle_1"))), 2, true);
			bundle2Bi = new BundleInfo("bundle_2", "1.0.0", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/bundle_2"))), 2, true);
		} catch (URISyntaxException e) {
			fail("3.0");
		} catch (IOException e) {
			fail("4.0");
		}

		manipulator.getConfigData().addBundle(osgiBi);
		manipulator.getConfigData().addBundle(bundle1Bi);
		manipulator.getConfigData().addBundle(bundle2Bi);
		try {
			manipulator.save(false);
		} catch (FrameworkAdminRuntimeException e) {
			fail("5.0");
		} catch (IOException e) {
			fail("6.0");
		}

		File configINI = new File(configurationFolder, "config.ini");
		assertContent(configINI, "org.eclipse.osgi");
		assertContent(configINI, "bundle_1");
		assertContent(configINI, "bundle_2");
		assertContent(configINI, "start,reference");	//This test for the presence of the comma.
	}
}
