/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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
import org.junit.Test;
import org.osgi.framework.BundleException;

public class Bug258370 extends FwkAdminAndSimpleConfiguratorTest {

	@Test
	public void testComma() throws FrameworkAdminRuntimeException, IOException, URISyntaxException, BundleException {
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = Activator.getContext().getDataFile(SimpleConfiguratorTest.class.getName());
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

		BundleInfo osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1",
				URIUtil.toURI(FileLocator
						.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar"))),
				0, true);
		BundleInfo bundle1Bi = new BundleInfo("bundle_1", "1.0.0",
				URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/bundle_1"))), 2,
				true);
		BundleInfo bundle2Bi = new BundleInfo("bundle_2", "1.0.0",
				URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/bundle_2"))), 2,
				true);

		manipulator.getConfigData().addBundle(osgiBi);
		manipulator.getConfigData().addBundle(bundle1Bi);
		manipulator.getConfigData().addBundle(bundle2Bi);
		manipulator.save(false);

		File configINI = new File(configurationFolder, "config.ini");
		assertContent(configINI, "org.eclipse.osgi");
		assertContent(configINI, "bundle_1");
		assertContent(configINI, "bundle_2");
		assertContent(configINI, "start,reference"); // This test for the presence of the comma.
	}
}
