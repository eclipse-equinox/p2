/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.junit.Test;
import org.osgi.framework.BundleException;

public class ReaderTestBug267850 extends AbstractFwkAdminTest {
	private File installFolder = null;
	private final String launcherName = "eclipse";

	@Test
	public void testConfigContent() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException {
		startSimpleConfiguratorManipulator();

		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		installFolder = getTestData(ReaderTestBug267850.class.getName(),"dataFile/readerTestBug267850");

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setLauncher(new File(installFolder, launcherName));
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}
		LauncherData data = manipulator.getLauncherData();
		String[] args = data.getProgramArgs();
		assertEquals("-startup", args[0]);
//		assertEquals("file:/C:/1target/provmiddle/org.eclipse.equinox.frameworkadmin.test/dataFile/readerTestBug267850/plugins/org.eclipse.equinox.launcher_1.0.200.v20090306-1900.jar", args[1]);
		assertEquals("--launcher.library", args[2]);
//		assertEquals("file:/C:/1target/provmiddle/org.eclipse.equinox.frameworkadmin.test/dataFile/readerTestBug267850/plugins/org.eclipse.equinox.launcher.win32.win32.x86_1.0.200.v20090306-1900", args[3]);
		assertEquals("-showsplash", args[4]);
		assertEquals("org.eclipse.platform", args[5]);
		assertEquals("--launcher.XXMaxPermSize", args[6]);
		assertEquals("256m", args[7]);
	}

}
