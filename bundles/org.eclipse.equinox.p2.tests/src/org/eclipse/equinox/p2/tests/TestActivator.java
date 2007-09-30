/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

public class TestActivator implements BundleActivator {
	public static final String PI_PROV_TESTS = "org.eclipse.equinox.p2.test";
	public static BundleContext context;
	private static PackageAdmin packageAdmin = null;
	private static ServiceReference packageAdminRef = null;

	public static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext context) throws Exception {
		TestActivator.context = context;
		packageAdminRef = context.getServiceReference(PackageAdmin.class.getName());
		packageAdmin = (PackageAdmin) context.getService(packageAdminRef);

		//This is a hack because the junit plugin launch config do not allow to start bundles
		getBundle("org.eclipse.equinox.p2.examplarysetup").start();
		getBundle("org.eclipse.equinox.frameworkadmin.equinox").start();
		getBundle("org.eclipse.equinox.simpleconfigurator.manipulator").start();
	}

	public void stop(BundleContext context) throws Exception {
		TestActivator.context = null;
	}

	public static Bundle getBundle(String symbolicName) {
		if (packageAdmin == null)
			return null;
		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		//Return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++) {
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundles[i];
			}
		}
		return null;
	}

}
