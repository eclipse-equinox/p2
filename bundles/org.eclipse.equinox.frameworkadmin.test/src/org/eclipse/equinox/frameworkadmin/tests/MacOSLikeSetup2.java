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
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.osgi.service.environment.Constants;
import org.osgi.framework.BundleException;

public class MacOSLikeSetup2 extends FwkAdminAndSimpleConfiguratorTest {

	public MacOSLikeSetup2(String name) {
		super(name);
	}

	public void testMacOSSetup() throws FrameworkAdminRuntimeException, IOException, BundleException {
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = new File("/home/pascal/git/rt.equinox.p2/bundles/org.eclipse.equinox.frameworkadmin.test/dataFile/mac/Eclipse.app/Contents/Eclipse");
		File configurationFolder = new File(installFolder, "configuration");
		File launcherFolder = new File(installFolder, "../MacOS/"); 
		File launcherName = new File(launcherFolder, "eclipse");

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configurationFolder);
		launcherData.setLauncher(launcherName);
//		launcherData.setLauncherConfigLocation(new File(installFolder, "eclipse.ini"));
		launcherData.setOS(Constants.OS_MACOSX);

		manipulator.load();
		
		assertTrue(manipulator.getLauncherData().getJvmArgs().length > 0);
	}
}
