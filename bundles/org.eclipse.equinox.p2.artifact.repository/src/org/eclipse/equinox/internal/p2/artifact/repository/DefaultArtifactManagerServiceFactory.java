/*******************************************************************************
 *  Copyright (c) 2025 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.OutputStream;
import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.AgentServiceName;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.osgi.service.component.annotations.Component;

/**
 * The default implementation of a {@link ArtifactManager} simply delegates to
 * the transport.
 */
@Component(service = IAgentServiceFactory.class)
@AgentServiceName(ArtifactManager.class)
public class DefaultArtifactManagerServiceFactory implements IAgentServiceFactory {

	@Override
	public Object createService(IProvisioningAgent agent) {
		return new DefaultArtifactManager(agent);
	}

	private static final class DefaultArtifactManager implements ArtifactManager {
		private IProvisioningAgent agent;

		public DefaultArtifactManager(IProvisioningAgent agent) {
			this.agent = agent;
		}

		@Override
		public IStatus getArtifact(URI source, OutputStream target, IArtifactDescriptor descriptor,
				IProgressMonitor monitor) {
			Transport transport = agent.getService(Transport.class);
			if (transport == null) {
				return Status.CANCEL_STATUS;
			}
			return transport.downloadArtifact(source, target, descriptor, monitor);
		}
	}
}
