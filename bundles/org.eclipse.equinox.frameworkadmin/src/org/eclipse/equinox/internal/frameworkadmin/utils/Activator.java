/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.utils;

import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.osgi.framework.*;

/**
 * @since 1.0
 */
public class Activator implements BundleActivator {

	private static BundleContext bundleContext;

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
	}

	/*
	 * Acquire the plug-in conversion service or return <code>null</code> if it is not available.
	 */
	public static PluginConverter acquirePluginConverter() {
		if (bundleContext == null)
			return null;
		ServiceReference<PluginConverter> reference = bundleContext.getServiceReference(PluginConverter.class);
		if (reference == null)
			return null;
		PluginConverter result = bundleContext.getService(reference);
		bundleContext.ungetService(reference);
		return result;
	}

}
