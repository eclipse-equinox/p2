/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype Inc - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.AgentServiceName;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.osgi.service.component.annotations.Component;

/**
 * Service factory for creating {@link IMetadataRepositoryManager} instances.
 */
@Component(service = IAgentServiceFactory.class, name = "org.eclipse.equinox.p2.metadata.repository")
@AgentServiceName(IMetadataRepositoryManager.class)
public class MetadataRepositoryComponent implements IAgentServiceFactory {

	@Override
	public Object createService(IProvisioningAgent agent) {
		return new MetadataRepositoryManager(agent);
	}
}
