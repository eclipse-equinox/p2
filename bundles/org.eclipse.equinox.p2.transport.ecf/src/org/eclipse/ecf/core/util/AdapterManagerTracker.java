/****************************************************************************
 * Copyright (c) 2014 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.core.util;

import org.eclipse.core.runtime.IAdapterManager;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @since 3.4
 */
@SuppressWarnings("rawtypes")
public class AdapterManagerTracker extends ServiceTracker {

	@SuppressWarnings("unchecked")
	public AdapterManagerTracker(BundleContext context, ServiceTrackerCustomizer customizer) {
		super(context, IAdapterManager.class.getName(), customizer);
	}

	public AdapterManagerTracker(BundleContext context) {
		this(context, null);
	}

	public IAdapterManager getAdapterManager() {
		IAdapterManager adapterManager = (IAdapterManager) getService();
		// Then, if the service isn't there, try to get from Platform class via
		// PlatformHelper class
		if (adapterManager == null)
			adapterManager = PlatformHelper.getPlatformAdapterManager();
		return adapterManager;
	}

}
