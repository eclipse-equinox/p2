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
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.garbagecollector;

import org.osgi.framework.*;

public class GarbageCollectorHelper {

	public static final String ID = "org.eclipse.equinox.p2.garbagecollector"; //$NON-NLS-1$
	public static final String GC_ENABLED = "gc_enabled"; //$NON-NLS-1$


	static <T> T getService(Class<T> clazz) {
		BundleContext context = FrameworkUtil.getBundle(GarbageCollectorHelper.class).getBundleContext();
		ServiceReference<T> reference = context.getServiceReference(clazz);
		if (reference == null) {
			return null;
		}
		T result = context.getService(reference);
		context.ungetService(reference);
		return result;
	}



}
