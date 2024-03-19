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
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.AgentServicename;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.osgi.service.component.annotations.Component;

/**
 * Instantiates default instances of {@link IProfileRegistry}.
 */
@Component(name = "org.eclipse.equinox.p2.engine.registry", service = IAgentServiceFactory.class)
@AgentServicename(IProfileRegistry.class)
public class ProfileRegistryComponent implements IAgentServiceFactory {

	@Override
	public Object createService(IProvisioningAgent agent) {
		IAgentLocation location = agent.getService(IAgentLocation.class);
		SimpleProfileRegistry registry = new SimpleProfileRegistry(agent, SimpleProfileRegistry.getDefaultRegistryDirectory(location));
		registry.setEventBus(agent.getService(IProvisioningEventBus.class));
		return registry;
	}
}
