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

import org.eclipse.equinox.internal.p2.core.ProvisioningEventBus;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.SimpleDirector;
import org.eclipse.equinox.internal.p2.director.SimplePlanner;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.p2.garbagecollector.GarbageCollector;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.osgi.framework.*;

public class Activator implements BundleActivator {
	public static BundleContext context;

	private IProvisioningEventBus bus;
	private ServiceRegistration registrationBus;

	private ServiceRegistration registrationDefaultManager;

	//	private ArtifactRepositoryManager artifactRepoManager;
	//	private ServiceRegistration registrationArtifactRepoManager;

	private SimpleProfileRegistry profileRegistry;
	private ServiceRegistration registrationProfileRegistry;

	private IDirector director;
	private ServiceRegistration registrationDirector;

	private IPlanner planner;
	private ServiceRegistration registrationPlanner;

	private AgentLocation agentLocation;

	private ServiceReference metadataRepositoryReference;

	private MetadataRepositoryManager metadataRepositoryManager;

	public void start(BundleContext aContext) throws Exception {
		//Need to do the configuration of all the bits and pieces:
		Activator.context = aContext;

		registerAgentLocation();
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

	private void registerAgentLocation() {
		//currently this is defined by p2.core but will be defined by the agent in the future
		//for now continue to treat it as a singleton
		agentLocation = (AgentLocation) ServiceHelper.getService(context, AgentLocation.SERVICE_NAME);
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
		Activator.context = null;

	}

	private void registerDirector() {
		IEngine engine = (IEngine) ServiceHelper.getService(context, IEngine.SERVICE_NAME);
		director = new SimpleDirector(engine, planner);
		registrationDirector = context.registerService(IDirector.SERVICE_NAME, director, null);
	}

	private void unregisterDirector() {
		registrationDirector.unregister();
		registrationDirector = null;
		director = null;
	}

	private void registerPlanner() {
		planner = new SimplePlanner(profileRegistry, metadataRepositoryManager);
		registrationPlanner = context.registerService(IPlanner.SERVICE_NAME, planner, null);
	}

	private void unregisterPlanner() {
		registrationPlanner.unregister();
		registrationPlanner = null;
		planner = null;
	}

	private void registerProfileRegistry() {
		profileRegistry = new SimpleProfileRegistry(SimpleProfileRegistry.getDefaultRegistryDirectory(agentLocation));
		profileRegistry.setEventBus(bus);
		registrationProfileRegistry = context.registerService(IProfileRegistry.class.getName(), profileRegistry, null);
		Engine engine = (Engine) ServiceHelper.getService(context, IEngine.SERVICE_NAME);
		if (engine != null)
			engine.setProfileRegistry(profileRegistry);
	}

	private void unregisterProfileRegistry() {
		registrationProfileRegistry.unregister();
		registrationProfileRegistry = null;
		profileRegistry = null;
	}

	/**
	 * Returns a metadata repository manager, registering a service if there isn't
	 * one registered already.
	 */
	private void registerMetadataRepositoryManager() {
		//register a metadata repository manager if there isn't one already registered
		metadataRepositoryReference = context.getServiceReference(IMetadataRepositoryManager.SERVICE_NAME);
		if (metadataRepositoryReference == null) {
			metadataRepositoryManager = new MetadataRepositoryManager();
			metadataRepositoryManager.setEventBus(bus);
			registrationDefaultManager = context.registerService(IMetadataRepositoryManager.SERVICE_NAME, metadataRepositoryManager, null);
			metadataRepositoryReference = registrationDefaultManager.getReference();
		} else {
			metadataRepositoryManager = (MetadataRepositoryManager) context.getService(metadataRepositoryReference);
		}
	}

	private void unregisterDefaultMetadataRepoManager() {
		//unget the service obtained for the metadata cache
		if (metadataRepositoryReference != null) {
			context.ungetService(metadataRepositoryReference);
			metadataRepositoryReference = null;
		}

		//unregister the service if we registered it
		if (registrationDefaultManager != null) {
			registrationDefaultManager.unregister();
			registrationDefaultManager = null;
		}
	}

	private void registerEventBus() {
		bus = new ProvisioningEventBus();
		registrationBus = context.registerService(IProvisioningEventBus.SERVICE_NAME, bus, null);
	}

	private void unregisterEventBus() {
		registrationBus.unregister();
		registrationBus = null;
		bus.close();
	}
}
