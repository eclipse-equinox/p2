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
package org.eclipse.equinox.internal.p2.installer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * The activator class controls the plug-in life cycle
 */
public class InstallerActivator implements BundleActivator {
	// The shared instance
	private static InstallerActivator plugin;
	// The plug-in ID
	public static final String PI_INSTALLER = "org.eclipse.equinox.p2.installer";

	private BundleContext context;

	private PackageAdmin packageAdmin = null;

	private ServiceReference packageAdminRef = null;

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static InstallerActivator getDefault() {
		return plugin;
	}

	/**
	 * The constructor
	 */
	public InstallerActivator() {
		//nothing to do
	}

	public Bundle getBundle(String symbolicName) {
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

	/**
	 * Returns the bundle context for this bundle.
	 * @return the bundle context
	 */
	public BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext aContext) throws Exception {
		this.context = aContext;
		plugin = this;
		packageAdminRef = context.getServiceReference(PackageAdmin.class.getName());
		packageAdmin = (PackageAdmin) context.getService(packageAdminRef);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext aContext) throws Exception {
		context.ungetService(packageAdminRef);
		packageAdmin = null;
		packageAdminRef = null;
		plugin = null;
	}

}
