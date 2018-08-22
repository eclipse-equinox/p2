/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

import java.io.File;
import java.io.IOException;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.BundleException;

public class ReaderTest3 extends AbstractFwkAdminTest {
	private File installFolder = null;
	private String launcherName = "eclipse";

	public ReaderTest3(String name) {
		super(name);
	}

	public void testConfigContent() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException {
		startSimpleConfiguratorManipulator();

		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		installFolder = getTestData(ReaderTest3.class.getName(),"dataFile/readerTest3");

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setLauncher(new File(installFolder, launcherName));
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}
		assertEquals(new File(installFolder, "configuration"), manipulator.getLauncherData().getFwConfigLocation()); 
		assertEquals("bar", manipulator.getConfigData().getProperty("foo"));
	}
}
