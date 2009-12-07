/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.exemplarysetup;

import org.eclipse.equinox.p2.engine.IProfileRegistry;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.garbagecollector.GarbageCollector;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.osgi.framework.*;

public class Activator implements BundleActivator {
	public static BundleContext context;
	public static final String ID = "org.eclipse.equinox.p2.exemplarysetup"; //$NON-NLS-1$

	private IProvisioningAgent agent;
	private IProvisioningEventBus bus;

	private ServiceRegistration registrationBus;
	private ServiceRegistration registrationDefaultManager;
	private ServiceRegistration registrationDirector;
	private ServiceRegistration registrationPlanner;
	private ServiceRegistration registrationProfileRegistry;

	/**
	 * Register the agent instance representing the currently running system.
	 * This will be the "default" agent for anyone not specifically trying to manipulate
	 * a different p2 agent location
	 */
	private void registerAgent() {
		//currently location is defined by p2.core but will be defined by the agent in the future
		//for now continue to treat it as a singleton
		ServiceReference locationRef = context.getServiceReference(IAgentLocation.SERVICE_NAME);
		if (locationRef == null)
			throw new RuntimeException("Unable to instantiate p2 agent because agent location is not available"); //$NON-NLS-1$
		IAgentLocation location = (IAgentLocation) context.getService(locationRef);
		if (location == null)
			throw new RuntimeException("Unable to instantiate p2 agent because agent location is not available"); //$NON-NLS-1$

		ServiceReference agentProviderRef = context.getServiceReference(IProvisioningAgentProvider.SERVICE_NAME);
		IProvisioningAgentProvider provider = (IProvisioningAgentProvider) context.getService(agentProviderRef);
		try {
			agent = provider.createAgent(null);
		} catch (Exception e) {
			//we can't proceed without an agent, so fail early
			final String msg = "Unable to instantiate p2 agent at location " + location.getRootLocation(); //$NON-NLS-1$
			LogHelper.log(new Status(IStatus.ERROR, ID, msg, e));
			throw new RuntimeException(msg);
		}

	}

	private void registerDirector() {
		IDirector director = (IDirector) agent.getService(IDirector.SERVICE_NAME);
		registrationDirector = context.registerService(IDirector.SERVICE_NAME, director, null);
	}

	private void registerEventBus() {
		bus = (IProvisioningEventBus) agent.getService(IProvisioningEventBus.SERVICE_NAME);
		registrationBus = context.registerService(IProvisioningEventBus.SERVICE_NAME, bus, null);
	}

	/**
	 * Returns a metadata repository manager, registering a service if there isn't
	 * one registered already.
	 */
	private void registerMetadataRepositoryManager() {
		//make sure there isn't a repository manager already registered
		if (context.getServiceReference(IMetadataRepositoryManager.SERVICE_NAME) == null) {
			IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
			registrationDefaultManager = context.registerService(IMetadataRepositoryManager.SERVICE_NAME, manager, null);
		}
	}

	private void registerPlanner() {
		IPlanner planner = (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
		registrationPlanner = context.registerService(IPlanner.SERVICE_NAME, planner, null);
	}

	private void registerProfileRegistry() {
		IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		registrationProfileRegistry = context.registerService(IProfileRegistry.SERVICE_NAME, profileRegistry, null);
	}

	public void start(BundleContext aContext) throws Exception {
		//Need to do the configuration of all the bits and pieces:
		Activator.context = aContext;

		registerAgent();
		registerEventBus();
		//create the profile registry
		registerProfileRegistry();
		registerMetadataRepositoryManager();

		//create the director and planner.  The planner must be
		//registered first because the director finds it in its constructor.
		registerPlanner();
		registerDirector();
		startGarbageCollector();

		//create artifact repositories
		//		registerDefaultArtifactRepoManager();
	}

	private void startGarbageCollector() {
		new GarbageCollector();
	}

	public void stop(BundleContext aContext) throws Exception {
		//		unregisterDefaultArtifactRepoManager();
		unregisterDirector();
		unregisterPlanner();
		unregisterDefaultMetadataRepoManager();
		unregisterProfileRegistry();
		unregisterEventBus();
		unregisterAgent();
		Activator.context = null;

	}

	private void unregisterAgent() {
		if (agent != null) {
			agent.stop();
			agent = null;
		}
	}

	private void unregisterDefaultMetadataRepoManager() {
		//unregister the service if we registered it
		if (registrationDefaultManager != null) {
			registrationDefaultManager.unregister();
			registrationDefaultManager = null;
		}
	}

	private void unregisterDirector() {
		registrationDirector.unregister();
		registrationDirector = null;
	}

	private void unregisterEventBus() {
		if (registrationBus != null) {
			registrationBus.unregister();
			registrationBus = null;
		}
		if (bus != null) {
			bus.close();
			bus = null;
		}
	}

	private void unregisterPlanner() {
		registrationPlanner.unregister();
		registrationPlanner = null;
	}

	private void unregisterProfileRegistry() {
		registrationProfileRegistry.unregister();
		registrationProfileRegistry = null;
	}
}
