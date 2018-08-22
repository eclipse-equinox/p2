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
import java.util.Properties;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.BundleException;

public class ReaderTest2 extends AbstractFwkAdminTest {
	private File installFolder = null;
	private File configurationFolder = null;
	private String launcherName = "eclipse";

	public ReaderTest2(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		startSimpleConfiguratorManipulator();

		installFolder = Activator.getContext().getDataFile(ReaderTest2.class.getName());
		configurationFolder = new File(installFolder, "conf");
		writeEclipseIni(new File(installFolder, "eclipse.ini"), new String[] { "-configuration", configurationFolder.getAbsolutePath() });
		Properties properties = new Properties();
		properties.setProperty("foo", "bar");
		writeConfigIni(new File(configurationFolder, "config.ini"), properties);
	}

	public void testConfigContent() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException {
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();
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

}
