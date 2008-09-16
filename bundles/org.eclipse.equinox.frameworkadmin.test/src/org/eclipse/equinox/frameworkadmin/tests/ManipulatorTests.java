/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;

public class ManipulatorTests extends AbstractFwkAdminTest {

	public ManipulatorTests(String name) {
		super(name);
	}

	public void testBug212361_osgiInBundlesList() throws Exception {
		File installFolder = Activator.getContext().getDataFile("212361");
		File configurationFolder = new File( installFolder, "configuration");
		Manipulator manipulator = getFrameworkManipulator(configurationFolder, new File(installFolder, "foo"));
		
		BundleInfo osgiBi = new BundleInfo("org.eclipse.osgi", "3.3.1", FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.osgi.jar")).toExternalForm(), 0, true);
		BundleInfo configuratorBi = new BundleInfo("org.eclipse.equinox.simpleconfigurator", "1.0.0", FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar")).toExternalForm(), 1, true);

		manipulator.getConfigData().addBundle(osgiBi);
		manipulator.getConfigData().addBundle(configuratorBi);
		
		manipulator.save(false);
		
		Properties configIni = new Properties();
		InputStream in = new BufferedInputStream(new FileInputStream(new File(configurationFolder, "config.ini")) );
		configIni.load(in);
		in.close();
		
		String bundles = (String) configIni.get("osgi.bundles");
		assertTrue(bundles.indexOf("org.eclipse.osgi") == -1);
	}

}
