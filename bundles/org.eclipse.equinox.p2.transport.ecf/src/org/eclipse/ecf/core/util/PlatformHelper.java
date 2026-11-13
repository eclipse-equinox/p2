/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.ecf.core.util;

import java.lang.reflect.Method;
import org.eclipse.core.runtime.*;
import org.eclipse.ecf.internal.core.identity.Activator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Helper class for eliminating direct references to Platform static methods
 * getAdapterManager and getExtensionRegistry. Note that instead of
 * Platform.getAdapterManager(), clients can call
 * PlatformHelper.getAdapterManager(). If this returns null, the Platform class
 * is not available.
 */
public class PlatformHelper {

	@SuppressWarnings("rawtypes")
	private static Class platformClass = null;

	private static IAdapterManager adapterManagerCache = null;

	private static IExtensionRegistry extensionRegistryCache = null;

	static {
		Activator a = Activator.getDefault();
		if (a != null) {
			try {
				BundleContext c = a.getBundleContext();
				if (c != null) {
					Bundle[] bundles = c.getBundles();
					Bundle coreRuntime = null;
					for (Bundle bundle : bundles) {
						if (bundle.getSymbolicName().equals("org.eclipse.core.runtime")) { //$NON-NLS-1$
							coreRuntime = bundle;
							platformClass = coreRuntime.loadClass("org.eclipse.core.runtime.Platform"); //$NON-NLS-1$
							break;
						}
					}
				}
			} catch (Exception e) {
				// Platform not available...just leave platformClass == null and
				// log
				// as error
				try {
					a.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, IStatus.WARNING,
							"Cannot load Platform class", //$NON-NLS-1$
							e));
				} catch (Throwable t) {
					// Should never happen, if does irrecoverable
				}
			}
		}
	}

	public synchronized static boolean isPlatformAvailable() {
		return platformClass != null;
	}

	public synchronized static IAdapterManager getPlatformAdapterManager() {
		if (adapterManagerCache != null)
			return adapterManagerCache;
		if (isPlatformAvailable()) {
			try {
				@SuppressWarnings("unchecked")
				Method m = platformClass.getMethod("getAdapterManager", (Class[]) null); //$NON-NLS-1$
				adapterManagerCache = (IAdapterManager) m.invoke(null, (Object[]) null);
				return adapterManagerCache;
			} catch (Exception e) {
				Activator.getDefault().log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, IStatus.WARNING,
						"Cannot get PlatformAdapterManager()", e)); //$NON-NLS-1$
				return null;
			}
		}
		return null;
	}

	public synchronized static IExtensionRegistry getExtensionRegistry() {
		if (extensionRegistryCache != null)
			return extensionRegistryCache;
		if (isPlatformAvailable()) {
			try {
				@SuppressWarnings("unchecked")
				Method m = platformClass.getMethod("getExtensionRegistry", (Class[]) null); //$NON-NLS-1$
				extensionRegistryCache = (IExtensionRegistry) m.invoke(null, (Object[]) null);
				return extensionRegistryCache;
			} catch (Exception e) {
				Activator.getDefault().log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, IStatus.WARNING,
						"Cannot get PlatformExtensionRegistry()", e)); //$NON-NLS-1$
				return null;
			}

		}
		return null;
	}
}
