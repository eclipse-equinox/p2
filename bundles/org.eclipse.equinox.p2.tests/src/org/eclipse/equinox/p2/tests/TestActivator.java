/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

public class TestActivator implements BundleActivator {
	public static final String PI_PROV_TESTS = "org.eclipse.equinox.p2.test";
	public static BundleContext context;
	private static PackageAdmin packageAdmin = null;
	private static ServiceReference<PackageAdmin> packageAdminRef = null;
	public static String TEST_DATA_PATH = "testData"; //$NON-NLS-1$

	public static BundleContext getContext() {
		return context;
	}

	/*
	 * Return a file handle to the framework log file, or null if it is not available.
	 */
	public static File getLogFile() {
		FrameworkLog log = ServiceHelper.getService(context, FrameworkLog.class);
		return log == null ? null : log.getFile();
	}

	@Override
	public void start(BundleContext ctx) throws Exception {
		TestActivator.context = ctx;
		packageAdminRef = ctx.getServiceReference(PackageAdmin.class);
		packageAdmin = ctx.getService(packageAdminRef);

		//This is a hack because the junit plugin launch config do not allow to start bundles
		AbstractProvisioningTest.startBundle(getBundle("org.eclipse.equinox.frameworkadmin.equinox"));
		AbstractProvisioningTest.startBundle(getBundle("org.eclipse.equinox.simpleconfigurator.manipulator"));
	}

	@Override
	public void stop(BundleContext ctx) throws Exception {
		TestActivator.context = null;
	}

	public static File getTestDataFolder() {
		try {
			URL url = context.getBundle().getEntry(TestActivator.TEST_DATA_PATH);
			return new File(FileLocator.resolve(url).getFile());
		} catch (IOException e) {
			return null;
		}
	}

	public static Bundle getBundle(String symbolicName) {
		if (packageAdmin == null)
			return null;
		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		//Return the first bundle that is not installed or uninstalled
		for (Bundle bundle : bundles) {
			if ((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundle;
			}
		}
		return null;
	}

}
