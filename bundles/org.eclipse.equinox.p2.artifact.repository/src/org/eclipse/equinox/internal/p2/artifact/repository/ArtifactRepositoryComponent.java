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
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;
import org.osgi.service.component.annotations.Component;

/**
 * Service factory providing {@link IArtifactRepositoryManager} instances.
 */
@Component(name = "org.eclipse.equinox.p2.artifact.repository", service = IAgentServiceFactory.class, property = IAgentServiceFactory.PROP_CREATED_SERVICE_NAME
		+ "=" + IArtifactRepositoryManager.SERVICE_NAME)
public class ArtifactRepositoryComponent implements IAgentServiceFactory {

	final List<ArtifactRepositoryFactory> artifactRepositories = new CopyOnWriteArrayList<>();

	@Override
	public Object createService(IProvisioningAgent agent) {
		return new ArtifactRepositoryManager(agent);
	}
}
