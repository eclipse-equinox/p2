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

public class SimpleConfiguratorComingAndGoing extends FwkAdminAndSimpleConfiguratorTest {
	Manipulator m = null;

	public SimpleConfiguratorComingAndGoing(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		m = createMinimalConfiguration(SimpleConfiguratorComingAndGoing.class.getName());
	}

	public void testWithMutipleBundles() throws IOException, BundleException, URISyntaxException {
		BundleInfo bi = new BundleInfo(URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/bundle_1"))), 2, false);
		m.getConfigData().addBundle(bi);
		m.save(false);

		BundleInfo[] bis = m.getConfigData().getBundles();
		for (int i = 0; i < bis.length; i++) {
			if (bis[i].getSymbolicName().equals("org.eclipse.equinox.simpleconfigurator"))
				m.getConfigData().removeBundle(bis[i]);
		}
		m.save(false);

		assertNothing(getBundleTxt());
		assertContent(getConfigIni(), "bundle_1");
		assertContent(getConfigIni(), "org.eclipse.osgi");

		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator newManipulator = fwkAdmin.getManipulator();

		LauncherData launcherData = newManipulator.getLauncherData();
		launcherData.setFwConfigLocation(getConfigurationFolder());
		launcherData.setLauncher(new File(getInstallFolder(), getLauncherName()));
		try {
			newManipulator.load();
		} catch (IllegalStateException e) {
			//TODO We ignore the framework JAR location not set exception
		}

		newManipulator.getConfigData().addBundle(new BundleInfo(URIUtil.toURI(FileLocator.resolve(Activator.getContext().getBundle().getEntry("dataFile/org.eclipse.equinox.simpleconfigurator.jar"))), 1, true));
		newManipulator.save(false);

		assertContent(getBundleTxt(), "org.eclipse.osgi");
		assertContent(getBundleTxt(), "bundle_1");
		assertContent(getBundleTxt(), "org.eclipse.equinox.simpleconfigurator");
	}
}
