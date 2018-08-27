/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.internal.p2.tests.verifier;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator implements BundleActivator {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.tests.verifier"; //$NON-NLS-1$
	private static BundleContext bundleContext;

	/**
	 * The constructor
	 */
	public Activator() {
		// nothing interesting to do
	}

	@Override
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
	}

	/*
	 * Return the bundle context or <code>null</code>.
	 */
	public static BundleContext getBundleContext() {
		return bundleContext;
	}

}
