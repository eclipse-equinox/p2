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
package org.eclipse.equinox.internal.p2.exemplarysetup;

import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.director.*;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.installregistry.IInstallRegistry;
import org.eclipse.equinox.p2.installregistry.InstallRegistry;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.osgi.framework.*;

public class Activator implements BundleActivator {
	public static final String ID = "org.eclipse.equinox.p2.core";
	public static BundleContext context;

	private ProvisioningEventBus bus;
	private ServiceRegistration registrationBus;

	private IInstallRegistry installRegistry;
	private ServiceRegistration registrationInstallRegistry;

	private MetadataRepositoryManager defaultManager;
	private ServiceRegistration registrationDefaultManager;

	//	private ArtifactRepositoryManager artifactRepoManager;
	//	private ServiceRegistration registrationArtifactRepoManager;

	private IProfileRegistry profileRegistry;
	private ServiceRegistration registrationProfileRegistry;

	private IDirector director;
	private ServiceRegistration registrationDirector;

	private IPlanner planner;
	private ServiceRegistration registrationPlanner;

	public void start(BundleContext context) throws Exception {
		//Need to do the configuration of all the bits and pieces:
		Activator.context = context;

		registerEventBus();
		//create the profile registry
		registerProfileRegistry();
		//create metadata repositories
		registerDefaultMetadataRepoManager();
		registerInstallRegistry();

		//create the director and planner.  The planner must be
		//registered first because the director finds it in its constructor.
		registerPlanner();
		registerDirector();

		//create artifact repositories
		//		registerDefaultArtifactRepoManager();
	}

	public void stop(BundleContext context) throws Exception {
		//		unregisterDefaultArtifactRepoManager();
		unregisterDirector();
		unregisterPlanner();
		unregisterInstallRegistry();
		unregisterDefaultMetadataRepoManager();
		unregisterProfileRegistry();
		unregisterEventBus();
		Activator.context = null;

	}

	private void registerDirector() {
		director = new SimpleDirector();
		registrationDirector = context.registerService(IDirector.class.getName(), director, null);
	}

	private void unregisterDirector() {
		registrationDirector.unregister();
		director = null;
	}

	private void registerPlanner() {
		planner = new SimplePlanner();
		registrationPlanner = context.registerService(IPlanner.class.getName(), planner, null);
	}

	private void unregisterPlanner() {
		registrationPlanner.unregister();
		planner = null;
	}

	private void registerProfileRegistry() {
		profileRegistry = new SimpleProfileRegistry();
		registrationProfileRegistry = context.registerService(IProfileRegistry.class.getName(), profileRegistry, null);
	}

	private void unregisterProfileRegistry() {
		registrationProfileRegistry.unregister();
		profileRegistry = null;
	}

	private void registerDefaultMetadataRepoManager() {
		defaultManager = new MetadataRepositoryManager();
		registrationDefaultManager = context.registerService(IMetadataRepositoryManager.class.getName(), defaultManager, null);
	}

	private void unregisterDefaultMetadataRepoManager() {
		registrationDefaultManager.unregister();
		registrationDefaultManager = null;
	}

	//	private void registerDefaultArtifactRepoManager() {
	//		artifactRepoManager = new ArtifactRepositoryManager();
	//		registrationArtifactRepoManager = context.registerService(IArtifactRepositoryManager.class.getName(), artifactRepoManager, null);
	//	}
	//
	//	private void unregisterDefaultArtifactRepoManager() {
	//		registrationArtifactRepoManager.unregister();
	//		artifactRepoManager = null;
	//	}

	private void registerInstallRegistry() {
		installRegistry = new InstallRegistry();
		registrationInstallRegistry = context.registerService(IInstallRegistry.class.getName(), installRegistry, null);
	}

	private void unregisterInstallRegistry() {
		registrationInstallRegistry.unregister();
		registrationInstallRegistry = null;
	}

	private void registerEventBus() {
		bus = new ProvisioningEventBus();
		registrationBus = context.registerService(ProvisioningEventBus.class.getName(), bus, null);
	}

	private void unregisterEventBus() {
		registrationBus.unregister();
		registrationBus = null;
		bus.close();
	}
}
