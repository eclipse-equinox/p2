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
import java.net.URISyntaxException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.BundleException;

public class NoRenamingLauncherIni extends AbstractFwkAdminTest {

	public NoRenamingLauncherIni(String name) {
		super(name);
	}

	public void testConfigFiles() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException, URISyntaxException {
		startSimpleConfiguratorManipulator();
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = Activator.getContext().getDataFile(NoRenamingLauncherIni.class.getName());
		File configurationFolder = new File(installFolder, "configuration");
		String launcherName = "foo";

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherName));
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}

		BundleInfo osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar"))), 0, true);
		BundleInfo configuratorBi = new BundleInfo("org.eclipse.equinox.simpleconfigurator", "1.0.0", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar"))), 1, true);

		manipulator.getConfigData().addBundle(osgiBi);
		manipulator.getConfigData().addBundle(configuratorBi);

		manipulator.save(false);

		File fooINI = new File(installFolder, "foo.ini");
		assertEquals(fooINI.exists(), true);

		Manipulator m2 = fwkAdmin.getManipulator();

		LauncherData launcherData2 = m2.getLauncherData();
		launcherData2.setFwConfigLocation(configurationFolder);
		launcherData2.setLauncher(new File(installFolder, launcherName));

		try {
			m2.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}
		launcherData2.setLauncher(new File(installFolder, "bar"));
		m2.save(false);

		assertEquals(fooINI.exists(), false);
		assertEquals(new File(installFolder, "bar.ini").exists(), true);
	}
}
