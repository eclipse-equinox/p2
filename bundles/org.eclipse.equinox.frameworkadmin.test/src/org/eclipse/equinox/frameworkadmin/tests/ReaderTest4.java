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
import java.util.Properties;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.BundleException;

public class ReaderTest4 extends AbstractFwkAdminTest {
	private File installFolder = null;
	private File configurationFolder = null;
	private String launcherName = "eclipse";

	public ReaderTest4(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		startSimpleConfiguratorManipulator();

		installFolder = Activator.getContext().getDataFile(ReaderTest4.class.getName());
		configurationFolder = new File(installFolder, "conf");
		writeEclipseIni(new File(installFolder, "eclipse.ini"), new String[] { "-install", installFolder.getAbsolutePath(), "-configuration", URIUtil.toUnencodedString(URIUtil.makeRelative(configurationFolder.toURI(), installFolder.toURI())) });
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

	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
