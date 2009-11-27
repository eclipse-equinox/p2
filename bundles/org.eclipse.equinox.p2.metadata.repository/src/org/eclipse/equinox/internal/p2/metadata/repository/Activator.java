/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc - additional implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {

	public static final String ID = "org.eclipse.equinox.p2.metadata.repository"; //$NON-NLS-1$
	public static final String REPO_PROVIDER_XPT = ID + '.' + "metadataRepositories"; //$NON-NLS-1$

	private static BundleContext bundleContext;
	//hack - currently set by MetadataRepositoryComponent
	static CacheManager cacheManager;
	private ServiceRegistration repositoryManagerRegistration;
	private ServiceTracker tracker;

	public static BundleContext getContext() {
		return bundleContext;
	}

	public static CacheManager getCacheManager() {
		return cacheManager;
	}

	public Object addingService(ServiceReference reference) {
		if (repositoryManagerRegistration == null) {
			//TODO: eventually we shouldn't register a singleton manager automatically
			IProvisioningAgent agent = (IProvisioningAgent) bundleContext.getService(reference);
			IMetadataRepositoryManager manager = (MetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
			repositoryManagerRegistration = bundleContext.registerService(IMetadataRepositoryManager.SERVICE_NAME, manager, null);
			return agent;
		}
		return null;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// nothing to do
	}

	public void removedService(ServiceReference reference, Object service) {
		if (repositoryManagerRegistration != null) {
			repositoryManagerRegistration.unregister();
			repositoryManagerRegistration = null;
		}
	}

	public void start(BundleContext aContext) throws Exception {
		bundleContext = aContext;
		//only want to register a service for the agent of the currently running system
		String filter = "(&(objectClass=" + IProvisioningAgent.SERVICE_NAME + ")(agent.current=true))"; //$NON-NLS-1$ //$NON-NLS-2$
		tracker = new ServiceTracker(aContext, aContext.createFilter(filter), this);
		tracker.open();
	}

	public void stop(BundleContext aContext) throws Exception {
		if (tracker != null) {
			tracker.close();
			tracker = null;
		}
		bundleContext = null;
	}
}
