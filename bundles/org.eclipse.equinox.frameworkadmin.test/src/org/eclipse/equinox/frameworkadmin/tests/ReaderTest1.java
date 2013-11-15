/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.BundleException;

public class ReaderTest1 extends AbstractFwkAdminTest {
	private File installFolder = null;
	private String launcherName = "eclipse";

	public ReaderTest1(String name) {
		super(name);
	}


	public void testConfigContent() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException {
		startSimpleConfiguratorManipulator();

		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		installFolder = getTestData(ReaderTest1.class.getName(),"dataFile/readerTest1");

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setLauncher(new File(installFolder, launcherName));
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}
		assertEquals(new File(installFolder, "conf"), manipulator.getLauncherData().getFwConfigLocation()); 
		assertEquals("bar", manipulator.getConfigData().getProperty("foo"));
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
