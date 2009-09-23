/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {
	public static final String ID = "org.eclipse.equinox.p2.artifact.repository"; //$NON-NLS-1$
	public static final String REPO_PROVIDER_XPT = ID + '.' + "artifactRepositories"; //$NON-NLS-1$

	private static BundleContext context;
	private ServiceRegistration repositoryManagerRegistration;
	private ArtifactRepositoryManager repositoryManager;
	private ServiceTracker busTracker;

	public static BundleContext getContext() {
		return Activator.context;
	}

	public void start(BundleContext aContext) throws Exception {
		Activator.context = aContext;
		repositoryManager = new ArtifactRepositoryManager();
		repositoryManagerRegistration = aContext.registerService(IArtifactRepositoryManager.class.getName(), repositoryManager, null);

		// need to track event bus coming and going to make sure cache gets cleaned on
		// repository removals
		busTracker = new ServiceTracker(context, IProvisioningEventBus.SERVICE_NAME, this);
		busTracker.open();
	}

	public void stop(BundleContext aContext) throws Exception {
		Activator.context = null;
		if (repositoryManagerRegistration != null)
			repositoryManagerRegistration.unregister();
		repositoryManagerRegistration = null;
		if (repositoryManager != null) {
			repositoryManager.shutdown();
			repositoryManager = null;
		}
		busTracker.close();
	}

	public Object addingService(ServiceReference reference) {
		IProvisioningEventBus bus = (IProvisioningEventBus) context.getService(reference);
		if (repositoryManager != null)
			repositoryManager.setEventBus(bus);
		return bus;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// ignored

	}

	public void removedService(ServiceReference reference, Object service) {
		if (repositoryManager != null)
			repositoryManager.unsetEventBus((IProvisioningEventBus) service);
	}

}
