/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.BundleException;

public class NoConfigurationValueInEclipseIni extends FwkAdminAndSimpleConfiguratorTest {

	public NoConfigurationValueInEclipseIni(String name) {
		super(name);
	}

	public void testAbsenceOfConfigurationInEclipseINI() throws Exception {
		createMinimalConfiguration(NoConfigurationValueInEclipseIni.class.getName());
		File launcherIni = new File(getInstallFolder(), getLauncherName() + ".ini");
		assertNotContent(launcherIni, "-configuration");
	}

	public void testPresenceOfConfigurationInEclipseINI() throws FrameworkAdminRuntimeException, IOException, BundleException, URISyntaxException {
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = Activator.getContext().getDataFile("bis" + NoConfigurationValueInEclipseIni.class.getName());
		File configurationFolder = new File(installFolder, "config2");
		String launcherName = "eclipse";

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherName));

		BundleInfo osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar"))), 0, true);
		BundleInfo configuratorBi = new BundleInfo("org.eclipse.equinox.simpleconfigurator", "1.0.0", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar"))), 1, true);

		manipulator.getConfigData().addBundle(osgiBi);
		manipulator.getConfigData().addBundle(configuratorBi);

		try {
			manipulator.save(false);
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}
		File launcherIni = new File(installFolder, launcherName + ".ini");
		assertContent(launcherIni, "-configuration");
	}
}
