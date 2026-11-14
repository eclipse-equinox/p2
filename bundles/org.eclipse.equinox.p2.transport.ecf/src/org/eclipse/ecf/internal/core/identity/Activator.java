/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ecf.internal.core.identity;

import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ecf.core.util.AdapterManagerTracker;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Replacement for the ECF identity bundle activator that is not started in
 * this bundle. It only provides what the copied ECF classes need to work and
 * does neither register the {@code IIDFactory} service nor read the
 * {@code org.eclipse.ecf.identity.namespace} extension point.
 */
public class Activator {

	public static final String PLUGIN_ID = "org.eclipse.ecf.identity"; //$NON-NLS-1$

	private static final Activator DEFAULT = new Activator();

	private AdapterManagerTracker adapterManagerTracker;

	public static Activator getDefault() {
		return DEFAULT;
	}

	public synchronized IAdapterManager getAdapterManager() {
		if (adapterManagerTracker == null) {
			BundleContext context = getBundleContext();
			if (context == null) {
				return null;
			}
			adapterManagerTracker = new AdapterManagerTracker(context);
			adapterManagerTracker.open();
		}
		return adapterManagerTracker.getAdapterManager();
	}

	/** Tracing is not supported by this replacement. */
	public DebugOptions getDebugOptions() {
		return null;
	}

	public BundleContext getBundleContext() {
		Bundle bundle = FrameworkUtil.getBundle(Activator.class);
		return bundle == null ? null : bundle.getBundleContext();
	}

	/** Logging is not supported by this replacement. */
	public void log(IStatus status) {
		// not supported
	}

	public void setupNamespaceExtensionPoint() {
		// namespaces from the extension point are not supported
	}
}
