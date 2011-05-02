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

import java.net.URI;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.BundleException;

public class SimpleConfiguratorTest extends AbstractFwkAdminTest {

	public SimpleConfiguratorTest(String name) {
		super(name);
	}

	public void testConfigFiles() throws IllegalStateException, FrameworkAdminRuntimeException, IOException, BundleException, URISyntaxException {
		startSimpleConfiguratorManipulator();
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		File installFolder = Activator.getContext().getDataFile(SimpleConfiguratorTest.class.getName());
		File configurationFolder = new File(installFolder, "configuration");
		String launcherName = "eclipse";

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

		File bundleTXT = new File(configurationFolder, "org.eclipse.equinox.simpleconfigurator/bundles.info");
		File configINI = new File(configurationFolder, "config.ini");
		assertContent(bundleTXT, "org.eclipse.osgi");
		assertContent(configINI, "org.eclipse.osgi");
		assertContent(bundleTXT, "org.eclipse.equinox.simpleconfigurator");
		assertContent(configINI, "org.eclipse.equinox.simpleconfigurator");
	}

	public void testBundleInfoEquals() throws Exception {
		BundleInfo b1 = new BundleInfo("org.foo", "3.1.0", new URI("plugins/org.foo_3.1.0"), -1, false);
		BundleInfo b2 = new BundleInfo("org.foo", "3.1.0", null, -1, false);
		BundleInfo b3 = new BundleInfo("org.foo", "3.1.0", URIUtil.fromString("C:/sp ace/plugins/org.foo_3.1.0"), -1, false);
		
		assertEquals(b1, b2);
		assertFalse(b1.equals(b3));
		
		b1.setBaseLocation(URIUtil.fromString("C:/sp ace"));
		assertEquals(b1, b3);
		
		b3.setBaseLocation(URIUtil.fromString("C:/sp ace"));
		assertEquals(b1, b3);
		
		b3.setVersion(null);
		b1.setVersion("0.0.0");
		assertEquals(b1, b3);
	}
}
