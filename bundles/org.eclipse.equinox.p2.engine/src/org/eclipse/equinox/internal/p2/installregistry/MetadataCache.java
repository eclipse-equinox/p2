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
package org.eclipse.equinox.internal.p2.installregistry;

import java.net.URL;
import java.util.ArrayList;
import java.util.EventObject;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.osgi.framework.ServiceReference;

public class MetadataCache {
	static final private String REPOSITORY_NAME = "Agent Metadata Cache"; //$NON-NLS-1$
	private ServiceReference busReference;
	private ProvisioningEventBus bus;
	private URL location;
	//tracks the IUs that have been installed but not yet committed
	//TODO: This will work if a single profile is being modified but we should consider how to handle multiple concurrent profile changes.OD
	final ArrayList toAdd = new ArrayList();

	public MetadataCache() {
		AgentLocation agentLocation = (AgentLocation) ServiceHelper.getService(EngineActivator.getContext(), AgentLocation.class.getName());
		location = (agentLocation != null ? agentLocation.getMetadataRepositoryURL() : null);
		hookListener();
	}

	AbstractMetadataRepository getRepository() {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(EngineActivator.getContext(), IMetadataRepositoryManager.class.getName());
		AbstractMetadataRepository repository = (AbstractMetadataRepository) manager.loadRepository(location, null);
		if (repository != null)
			return repository;
		//instruct the repository manager to construct a new metadata cache 
		repository = (AbstractMetadataRepository) manager.createRepository(location, REPOSITORY_NAME, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
		repository.setProperty(IRepository.IMPLEMENTATION_ONLY_KEY, Boolean.valueOf(true).toString());
		return repository;
	}

	private void hookListener() {
		// TODO: We should check for writing permission here, otherwise it may be too late
		busReference = EngineActivator.getContext().getServiceReference(ProvisioningEventBus.class.getName());
		bus = (ProvisioningEventBus) EngineActivator.getContext().getService(busReference);
		bus.addListener(new ProvisioningListener() {
			public void notify(EventObject o) {
				if (o instanceof InstallableUnitEvent) { //TODO This dependency on InstallableUnitEvent is not great
					InstallableUnitEvent event = (InstallableUnitEvent) o;
					if (event.isPre())
						return;
					// TODO: what about uninstall??
					if (event.isPost() && event.getResult().isOK() && event.isInstall()) {
						IInstallableUnit installedIU = event.getOperand().second();
						if (installedIU != null)
							toAdd.add(installedIU.unresolved());
						return;
					}
				}
				if (o instanceof CommitOperationEvent) {
					IInstallableUnit[] toAddArray = (IInstallableUnit[]) toAdd.toArray(new IInstallableUnit[toAdd.size()]);
					toAdd.clear();
					getRepository().addInstallableUnits(toAddArray);
				}
				if (o instanceof RollbackOperationEvent)
					toAdd.clear();
			}
		});
	}

}
