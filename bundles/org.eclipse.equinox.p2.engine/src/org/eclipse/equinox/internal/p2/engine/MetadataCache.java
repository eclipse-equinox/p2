/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.net.URI;
import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.osgi.framework.ServiceReference;

public class MetadataCache {
	static final private String REPOSITORY_NAME = "Agent Metadata Cache"; //$NON-NLS-1$
	private ServiceReference busReference;
	private IProvisioningEventBus bus;
	private URI location;
	//tracks the IUs that have been installed but not yet committed
	//TODO: This will work if a single profile is being modified but we should consider how to handle multiple concurrent profile changes.
	final ArrayList toAdd = new ArrayList();
	private final IMetadataRepositoryManager manager;

	public MetadataCache(IMetadataRepositoryManager manager) {
		this.manager = manager;
		AgentLocation agentLocation = (AgentLocation) ServiceHelper.getService(EngineActivator.getContext(), AgentLocation.class.getName());
		location = (agentLocation != null ? agentLocation.getMetadataRepositoryURI() : null);
		hookListener();
	}

	IMetadataRepository getRepository() {
		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		try {
			Map properties = new HashMap(1);
			properties.put(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
			return manager.createRepository(location, REPOSITORY_NAME, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		} catch (ProvisionException e) {
			LogHelper.log(e);
			throw new IllegalStateException(Messages.failed_creating_metadata_cache);
		}
	}

	private void hookListener() {
		// TODO: We should check for writing permission here, otherwise it may be too late
		busReference = EngineActivator.getContext().getServiceReference(IProvisioningEventBus.SERVICE_NAME);
		bus = (IProvisioningEventBus) EngineActivator.getContext().getService(busReference);
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
					if (toAddArray.length > 0)
						getRepository().addInstallableUnits(toAddArray);
				}
				if (o instanceof RollbackOperationEvent)
					toAdd.clear();
			}
		});
	}

}
