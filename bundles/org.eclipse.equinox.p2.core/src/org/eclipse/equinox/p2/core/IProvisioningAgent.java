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
package org.eclipse.equinox.p2.core;

import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;

/**
 * A provisioning agent is comprised of a modular, extensible set of related services.
 * Each agent instance has its own separate instances of these services that are
 * not shared with other agents. There is at most one instance of a given service
 * tracked by an agent at any given time, which ensures all services that make
 * up an agent instance share common service instances with each other.
 * <p>
 * Services are registered with an agent either directly, via the {@link #registerService(String, Object)}
 * method, or indirectly by registering an {@link IAgentServiceFactory} in the OSGi
 * service registry.
 * </p>
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 1.1
 */
public interface IProvisioningAgent {
	/**
	 * Service name constant for the agent service. Note that an agent obtained directly
	 * as a service typically represents the agent of the currently running system. To
	 * obtain an agent for a different system the {@link IProvisioningAgentProvider} 
	 * service must be used.
	 */
	public static final String SERVICE_NAME = IProvisioningAgent.class.getName();

	/**
	 * Returns the service with the given service name, or <code>null</code>
	 * if no such service is available in this agent.
	 */
	public Object getService(String serviceName);

	/**
	 * Registers a service with this provisioning agent.
	 * 
	 * @param serviceName The name of the service to register
	 * @param service The service implementation
	 */
	public void registerService(String serviceName, Object service);

	/**
	 * Unregisters a service that has previously been registered with this
	 * agent via {@link #registerService(String, Object)}. This method has
	 * no effect if no such service is registered with this agent.
	 * 
	 * @param serviceName The name of the service to unregister
	 * @param service The service implementation to unregister.
	 */
	public void unregisterService(String serviceName, Object service);

}