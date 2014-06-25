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

import java.io.*;
import java.util.Properties;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;

public class ManipulatorTests extends AbstractFwkAdminTest {

	public ManipulatorTests(String name) {
		super(name);
	}

	public void testBug212361_osgiInBundlesList() throws Exception {
		File installFolder = Activator.getContext().getDataFile("212361");
		File configurationFolder = new File(installFolder, "configuration");
		Manipulator manipulator = getFrameworkManipulator(configurationFolder, new File(installFolder, "foo"));

		BundleInfo osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar"))), 0, true);
		BundleInfo configuratorBi = new BundleInfo("org.eclipse.equinox.simpleconfigurator", "1.0.0", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar"))), 1, true);

		manipulator.getConfigData().addBundle(osgiBi);
		manipulator.getConfigData().addBundle(configuratorBi);

		manipulator.save(false);

		Properties configIni = new Properties();
		InputStream in = new BufferedInputStream(new FileInputStream(new File(configurationFolder, "config.ini")));
		configIni.load(in);
		in.close();

		String bundles = (String) configIni.get("osgi.bundles");
		assertTrue(bundles.indexOf("org.eclipse.osgi") == -1);
	}

	public void testBug277553_installAreaFromFwJar() throws Exception {
		File folder = getTestFolder("installAreaFromFwJar");
		File fwJar = new File(folder, "plugins/org.eclipse.osgi.jar");
		fwJar.getParentFile().mkdirs();

		copyStream(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar").openStream(), true, new FileOutputStream(fwJar), true);
		BundleInfo osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1", fwJar.toURI(), 0, true);
		
		File ini = new File(folder, "eclipse.ini");
		writeEclipseIni(ini, new String[] {"-foo", "bar", "-vmargs", "-Xmx256m"});
		
		startSimpleConfiguratorManipulator();
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		
		Manipulator manipulator = fwkAdmin.getManipulator();
		manipulator.getConfigData().addBundle(osgiBi);
		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwJar(fwJar);
		launcherData.setLauncher(new File(folder, "eclipse"));

		manipulator.load();
		
		assertEquals(manipulator.getLauncherData().getFwPersistentDataLocation(), new File(folder, "configuration"));
	}
	
	public void testBug258126_ProgramArgs_VMArgs() throws Exception {
		File installFolder = getTestFolder("258126");
		File ini = new File(installFolder, "eclipse.ini");
		writeEclipseIni(ini, new String[] {"-foo", "bar", "-vmargs", "-Xmx256m"});

		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();
		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setLauncher(new File(installFolder, "eclipse"));
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}

		assertEquals(launcherData.getJvmArgs(), new String[] {"-Xmx256m"});
		assertEquals(launcherData.getProgramArgs(), new String[] {"-foo", "bar"});

		launcherData.addJvmArg("-Xms64m");
		launcherData.addProgramArg("-console");

		//eclipse.ini won't save unless we actually have something in the configuration
		BundleInfo osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar"))), 0, true);
		manipulator.getConfigData().addBundle(osgiBi);
		manipulator.save(false);

		assertContent(ini, "-foo", "bar", "-console", "-vmargs", "-Xmx256m", "-Xms64m");
	}
	
}
