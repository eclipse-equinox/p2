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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.osgi.service.environment.Constants;
import org.osgi.framework.BundleException;

public abstract class FwkAdminAndSimpleConfiguratorTest extends AbstractFwkAdminTest {
	private File installFolder;
	private File configurationFolder;
	private String launcherName;
	private IPath launcherPath;

	public FwkAdminAndSimpleConfiguratorTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		startSimpleConfiguratorManipulator();
	}

	protected Manipulator getNewManipulator(String workArea) throws FrameworkAdminRuntimeException, IOException, BundleException {
		return getNewManipulator(workArea, null);
	}

	protected Manipulator getNewManipulator(String workArea, String os) throws FrameworkAdminRuntimeException, IOException, BundleException {
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		installFolder = getTestFolder(workArea, false);
		configurationFolder = new File(installFolder, "configuration");
		launcherName = "eclipse";

		boolean isMacOS = Constants.OS_MACOSX.equals(os);
		launcherPath = isMacOS ? new Path("../").append(launcherName) : new Path(launcherName);

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setHome(installFolder);
		launcherData.setFwConfigLocation(configurationFolder);
		launcherData.setLauncher(new File(installFolder, launcherPath.toOSString()));
		launcherData.setOS(os);
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}
		return manipulator;
	}

	protected Manipulator createMinimalConfiguration(String workArea) throws Exception {
		return createMinimalConfiguration(workArea, null);
	}

	protected Manipulator createMinimalConfiguration(String workArea, String os) throws Exception {
		Manipulator manipulator = getNewManipulator(workArea, os);

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

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (installFolder != null)
			delete(installFolder);
	}

	public File getInstallFolder() {
		return installFolder;
	}

	public File getLauncherConfigFile() {
		File launcherDir = new File(getInstallFolder(), launcherPath.toOSString()).getParentFile();
		return new File(launcherDir, getLauncherName() + ".ini");
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
