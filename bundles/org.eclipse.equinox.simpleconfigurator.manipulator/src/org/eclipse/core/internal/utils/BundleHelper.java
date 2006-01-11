/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.utils;

import org.osgi.framework.*;

public class BundleHelper implements BundleActivator {
	private BundleContext context;
	private static BundleHelper defaultInstance;
	
	public static BundleHelper getDefault() {
		return defaultInstance;
	}
	
	public BundleHelper() throws RuntimeException {
		if (defaultInstance != null)
			throw new RuntimeException("Can not instantiate bundle helper"); //$NON-NLS-1$
		defaultInstance = this;
	}

	static void shutdown() {
		if (defaultInstance != null) {
			defaultInstance.context = null;
			defaultInstance = null;
		}
	}
	
	public Object acquireService(String serviceName) {
		ServiceReference reference = context.getServiceReference(serviceName);
		if (reference == null)
			return null;
		return context.getService(reference);
	}

	public void start(BundleContext context) throws Exception {
		defaultInstance.context = context;
	}

	public void stop(BundleContext context) throws Exception {
		shutdown();
	}
}
