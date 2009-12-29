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

import java.util.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {
	public static final String ID = "org.eclipse.equinox.p2.artifact.repository"; //$NON-NLS-1$
	public static final String REPO_PROVIDER_XPT = ID + '.' + "artifactRepositories"; //$NON-NLS-1$

	private static BundleContext context;
	private ServiceRegistration repositoryManagerRegistration;
	private static final Map<ArtifactRepositoryManager, IProvisioningAgent> createdManagers = new HashMap<ArtifactRepositoryManager, IProvisioningAgent>();
	private ServiceTracker agentTracker;

	public static BundleContext getContext() {
		return Activator.context;
	}

	/**
	 * Remember an artifact repository manager so we can shut it down when the bundle stops
	 */
	static void addManager(ArtifactRepositoryManager manager, IProvisioningAgent agent) {
		synchronized (createdManagers) {
			createdManagers.put(manager, agent);
		}
	}

	public void start(BundleContext aContext) throws Exception {
		Activator.context = aContext;
		// need to track agent so we can register global artifact repository manager
		String filter = "(&(objectClass=" + IProvisioningAgent.SERVICE_NAME + ")(agent.current=true))"; //$NON-NLS-1$ //$NON-NLS-2$
		agentTracker = new ServiceTracker(context, aContext.createFilter(filter), this);
		agentTracker.open();
	}

	public void stop(BundleContext aContext) throws Exception {
		Activator.context = null;
		if (repositoryManagerRegistration != null)
			repositoryManagerRegistration.unregister();
		repositoryManagerRegistration = null;
		synchronized (createdManagers) {
			for (Iterator<ArtifactRepositoryManager> it = createdManagers.keySet().iterator(); it.hasNext();) {
				ArtifactRepositoryManager manager = it.next();
				manager.shutdown();
				IProvisioningAgent agent = createdManagers.get(manager);
				agent.unregisterService(IArtifactRepositoryManager.SERVICE_NAME, manager);
			}
			createdManagers.clear();
		}
		agentTracker.close();
	}

	public Object addingService(ServiceReference reference) {
		//when someone registers the agent service, register a repository manager service
		IProvisioningAgent agent = (IProvisioningAgent) context.getService(reference);
		repositoryManagerRegistration = context.registerService(IArtifactRepositoryManager.SERVICE_NAME, agent.getService(IArtifactRepositoryManager.SERVICE_NAME), null);
		return agent;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// ignored
	}

	public void removedService(ServiceReference reference, Object service) {
		//the agent is going away so withdraw our service
		if (repositoryManagerRegistration != null) {
			repositoryManagerRegistration.unregister();
			repositoryManagerRegistration = null;
		}
	}
}
