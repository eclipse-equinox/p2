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
import java.net.URISyntaxException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.BundleException;

public class Bug196525 extends AbstractFwkAdminTest {
	private File installFolder = null;
	private File configurationFolder = null;
	private String launcherName = "eclipse";
	private File bundleTXT;
	private File configINI;

	public Bug196525(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		startSimpleConfiguratorManipulator();
		//create a configuration with osgi and simpleconfigurator in it

		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		installFolder = Activator.getContext().getDataFile(Bug196525.class.getName());
		configurationFolder = new File(installFolder, "configuration");

		bundleTXT = new File(configurationFolder, "org.eclipse.equinox.simpleconfigurator/bundles.info");
		configINI = new File(configurationFolder, "config.ini");

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
	}

	public void testConfigContent() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException, URISyntaxException {
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherName));
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}

		try {
			assertContains("1.0", manipulator.getConfigData().getBundles(), URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar"))));
			assertContains("2.0", manipulator.getConfigData().getBundles(), URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar"))));
		} catch (URISyntaxException e) {
			fail("Unexpected failure while creating URI");
		}
		BundleInfo bundle1Bi = new BundleInfo("bundle_1", "1.0.0", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/bundle_1"))), 2, true);

		manipulator.getConfigData().addBundle(bundle1Bi);

		manipulator.save(false);

		assertContent(bundleTXT, "org.eclipse.osgi");
		assertContent(configINI, "org.eclipse.osgi");
		assertContent(bundleTXT, "org.eclipse.equinox.simpleconfigurator");
		assertContent(configINI, "org.eclipse.equinox.simpleconfigurator");
		assertContent(bundleTXT, "bundle_1");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
