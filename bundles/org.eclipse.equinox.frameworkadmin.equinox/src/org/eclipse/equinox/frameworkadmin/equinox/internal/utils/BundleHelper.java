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
package org.eclipse.equinox.frameworkadmin.equinox.internal.utils;

import java.io.File;

import org.osgi.framework.*;

public class BundleHelper implements BundleActivator {
	private static BundleHelper defaultInstance;
	public static BundleHelper getDefault() {
		return defaultInstance;
	}
	
	static void shutdown() {
		if (defaultInstance != null) {
			defaultInstance.context = null;
			defaultInstance = null;
		}
	}
	
	private BundleContext context;

	public BundleHelper() throws RuntimeException {
		if (defaultInstance != null)
			throw new RuntimeException("Can not instantiate bundle helper"); //$NON-NLS-1$
		defaultInstance = this;
	}
	
	public Object acquireService(String serviceName) {
		ServiceReference reference = context.getServiceReference(serviceName);
		if (reference == null)
			return null;
		return context.getService(reference);
	}

	public File getDataFile(String fileName) {
		return context.getDataFile(fileName);
	}

	public void start(BundleContext context) throws Exception {
		defaultInstance.context = context;
	}
	
	public void stop(BundleContext context) throws Exception {
		shutdown();
	}
}
