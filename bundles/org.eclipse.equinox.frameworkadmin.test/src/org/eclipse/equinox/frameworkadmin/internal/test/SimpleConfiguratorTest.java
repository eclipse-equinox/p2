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
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.frameworkadmin.*;
import org.osgi.framework.BundleException;

public class SimpleConfiguratorTest extends AbstractFwkAdminTest {

	public SimpleConfiguratorTest(String name) {
		super(name);
	}

	public void testConfigFiles() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException {
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = Activator.getContext().getDataFile(SimpleConfiguratorTest.class.getSimpleName());
		File configurationFolder = new File(installFolder, "configuration");
		String launcherName = "eclipse";

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherName));
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}

		BundleInfo osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1", FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar")).toExternalForm(), 0, true);
		BundleInfo configuratorBi = new BundleInfo("org.eclipse.equinox.simpleconfigurator", "1.0.0", FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar")).toExternalForm(), 1, true);

		manipulator.getConfigData().addBundle(osgiBi);
		manipulator.getConfigData().addBundle(configuratorBi);

		manipulator.save(false);

		File bundleTXT = new File(configurationFolder, "org.eclipse.equinox.simpleconfigurator/bundles.txt");
		File configINI = new File(configurationFolder, "config.ini");
		assertContent(bundleTXT, "org.eclipse.osgi");
		assertContent(configINI, "org.eclipse.osgi");
		assertContent(bundleTXT, "org.eclipse.equinox.simpleconfigurator");
		assertContent(configINI, "org.eclipse.equinox.simpleconfigurator");
	}
}
