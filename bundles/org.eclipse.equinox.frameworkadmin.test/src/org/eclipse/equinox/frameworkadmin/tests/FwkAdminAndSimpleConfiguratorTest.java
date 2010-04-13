/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.BundleException;

public abstract class FwkAdminAndSimpleConfiguratorTest extends AbstractFwkAdminTest {
	private File installFolder;
	private File configurationFolder;
	private String launcherName;

	public FwkAdminAndSimpleConfiguratorTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		startSimpleConfiguratorManipulator();
	}

	protected Manipulator getNewManipulator(String workArea) throws FrameworkAdminRuntimeException, IOException, BundleException {
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		installFolder = getTestFolder(workArea, false);
		configurationFolder = new File(installFolder, "configuration");
		launcherName = "eclipse";

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherName));
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}
		return manipulator;
	}

	protected Manipulator createMinimalConfiguration(String workArea) throws Exception {
		Manipulator manipulator = getNewManipulator(workArea);

		BundleInfo osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar"))), 0, true);
		BundleInfo configuratorBi = new BundleInfo("org.eclipse.equinox.simpleconfigurator", "1.0.0", URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar"))), 1, true);

		manipulator.getConfigData().addBundle(osgiBi);
		manipulator.getConfigData().addBundle(configuratorBi);

		try {
			manipulator.save(false);
		} catch (IOException e) {
			fail("Error while persisting");
		} catch (FrameworkAdminRuntimeException e) {
			fail("Error while persisting");
		}
		return manipulator;
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if (installFolder != null)
			delete(installFolder);
	}

	public File getInstallFolder() {
		return installFolder;
	}

	public File getConfigurationFolder() {
		return configurationFolder;
	}

	public File getBundleTxt() {
		return new File(configurationFolder, "org.eclipse.equinox.simpleconfigurator/bundles.info");
	}

	public File getConfigIni() {
		return new File(configurationFolder, "config.ini");
	}

	public String getLauncherName() {
		return launcherName;
	}
}
