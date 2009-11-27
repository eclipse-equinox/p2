/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;

/**
 * Service factory providing {@link IArtifactRepositoryManager} instances.
 */
public class ArtifactRepositoryComponent implements IAgentServiceFactory {

	public Object createService(IProvisioningAgent agent) {
		final ArtifactRepositoryManager manager = new ArtifactRepositoryManager();
		manager.setEventBus((IProvisioningEventBus) agent.getService(IProvisioningEventBus.SERVICE_NAME));
		manager.setAgentLocation((IAgentLocation) agent.getService(IAgentLocation.SERVICE_NAME));
		Activator.addManager(manager, agent);
		return manager;
	}
}
