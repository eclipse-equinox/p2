/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.installer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class InstallerActivator implements BundleActivator {
	// The plug-in ID
	public static final String PI_INSTALLER = "org.eclipse.equinox.p2.installer"; //$NON-NLS-1$
	// The shared instance
	private static InstallerActivator plugin;

	private BundleContext context;

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

	/**
	 * Returns the bundle context for this bundle.
	 * @return the bundle context
	 */
	public BundleContext getContext() {
		return context;
	}

	@Override
	public void start(BundleContext aContext) throws Exception {
		this.context = aContext;
		plugin = this;
	}

	@Override
	public void stop(BundleContext aContext) throws Exception {
		plugin = null;
	}

}
