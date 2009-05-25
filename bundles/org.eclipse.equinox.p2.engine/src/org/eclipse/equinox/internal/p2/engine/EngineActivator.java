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
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.engine.Engine;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class EngineActivator implements BundleActivator, ServiceTrackerCustomizer {
	private static BundleContext context;
	public static final String ID = "org.eclipse.equinox.p2.engine"; //$NON-NLS-1$

	private ServiceRegistration registration;

	private ServiceTracker tracker;

	public static BundleContext getContext() {
		return context;
	}

	public Object addingService(ServiceReference reference) {
		if (registration == null) {
			ProvisioningEventBus eventBus = (ProvisioningEventBus) context.getService(reference);
			registration = context.registerService(Engine.class.getName(), new Engine(eventBus), null);
			return eventBus;
		}
		return null;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// nothing to do
	}

	public void removedService(ServiceReference reference, Object service) {
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
	}

	public void start(BundleContext aContext) throws Exception {
		EngineActivator.context = aContext;
		tracker = new ServiceTracker(aContext, ProvisioningEventBus.class.getName(), this);
		tracker.open();
	}

	public void stop(BundleContext aContext) throws Exception {
		tracker.close();
		tracker = null;

		EngineActivator.context = null;
	}

}
