/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.garbagecollector;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.*;

public class GCActivator implements BundleActivator {
	public static final String ID = "org.eclipse.equinox.p2.garbagecollector"; //$NON-NLS-1$
	public static final String GC_ENABLED = "gc_enabled"; //$NON-NLS-1$
	private static final String DEBUG_STRING = GCActivator.ID + "/debug"; //$NON-NLS-1$
	private static final boolean DEFAULT_DEBUG = false;

	static BundleContext context;

	static <T> T getService(Class<T> clazz) {
		ServiceReference<T> reference = context.getServiceReference(clazz);
		if (reference == null)
			return null;
		T result = context.getService(reference);
		context.ungetService(reference);
		return result;
	}

	public void start(BundleContext inContext) throws Exception {
		GCActivator.context = inContext;
		DebugOptions debug = getService(DebugOptions.class);
		if (debug != null) {
			CoreGarbageCollector.setDebugMode(debug.getBooleanOption(DEBUG_STRING, DEFAULT_DEBUG));
		}
	}

	public void stop(BundleContext inContext) throws Exception {
		GCActivator.context = null;
	}
}
