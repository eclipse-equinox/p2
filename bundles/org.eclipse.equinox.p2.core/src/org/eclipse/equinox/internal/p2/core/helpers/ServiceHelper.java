/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.core.helpers;

import java.util.Collection;
import org.osgi.framework.*;

public class ServiceHelper {
	/**
	 * Returns the service described by the given arguments.  Note that this is a helper class
	 * that <b>immediately</b> ungets the service reference.  This results in a window where the
	 * system thinks the service is not in use but indeed the caller is about to use the returned
	 * service object.
	 * @param context
	 * @param clazz the service class
	 * @return The requested service
	 */
	public static <T> T getService(BundleContext context, Class<T> clazz) {
		if (context == null)
			return null;
		ServiceReference<T> reference = context.getServiceReference(clazz);
		if (reference == null)
			return null;
		T result = context.getService(reference);
		context.ungetService(reference);
		return result;
	}

	public static <T> T getService(BundleContext context, Class<T> clazz, String filter) {
		if (context == null)
			return null;
		Collection<ServiceReference<T>> references;
		try {
			references = context.getServiceReferences(clazz, filter);
		} catch (InvalidSyntaxException e) {
			return null;
		}
		if (references.isEmpty())
			return null;
		final ServiceReference<T> ref = references.iterator().next();
		T result = context.getService(ref);
		context.ungetService(ref);
		return result;
	}

	/**
	 * Returns the service described by the given arguments.  Note that this is a helper class
	 * that <b>immediately</b> ungets the service reference.  This results in a window where the
	 * system thinks the service is not in use but indeed the caller is about to use the returned
	 * service object.
	 * @param context
	 * @param name
	 * @return The requested service
	 */
	public static Object getService(BundleContext context, String name) {
		if (context == null)
			return null;
		ServiceReference<?> reference = context.getServiceReference(name);
		if (reference == null)
			return null;
		Object result = context.getService(reference);
		context.ungetService(reference);
		return result;
	}

	public static Object getService(BundleContext context, String name, String filter) {
		if (context == null)
			return null;
		ServiceReference<?>[] references;
		try {
			references = context.getServiceReferences(name, filter);
		} catch (InvalidSyntaxException e) {
			return null;
		}
		if (references == null || references.length == 0)
			return null;
		Object result = context.getService(references[0]);
		context.ungetService(references[0]);
		return result;
	}
}
