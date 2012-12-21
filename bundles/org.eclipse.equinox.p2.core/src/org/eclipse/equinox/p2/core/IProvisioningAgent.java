/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB (Pascal Rapicault) - reading preferences from base in shared install
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
 * @since 2.0
 */
public interface IProvisioningAgent {
	/**
	 * Service name constant for the agent service. Note that an agent obtained directly
	 * as a service typically represents the agent of the currently running system. To
	 * obtain an agent for a different system the {@link IProvisioningAgentProvider} 
	 * service must be used.
	 */
	public static final String SERVICE_NAME = IProvisioningAgent.class.getName();

	public static final String INSTALLER_AGENT = "org.eclipse.equinox.p2.installer.agent"; //$NON-NLS-1$
	public static final String INSTALLER_PROFILEID = "org.eclipse.equinox.p2.installer.profile.id"; //$NON-NLS-1$

	/** 
	 * When running in "shared mode", this allows to retrieve from the IProvisioningAgent the agent representing what is in the shared location aka the base
	 * @since 2.3
	 */
	public static final String SHARED_BASE_AGENT = "org.eclipse.equinox.shared.base.agent"; //$NON-NLS-1$

	/** 
	 * When running in "shared mode", this allows to retrieve from the IProvisioningAgent identified by {@link #SHARED_BASE_AGENT} the current agent 
	 * @since 2.3
	 */
	public static final String SHARED_CURRENT_AGENT = "org.eclipse.equinox.shared.current.agent"; //$NON-NLS-1$
	/**
	 * Service property identifying whether an agent is the default agent.
	 * 
	 * <p>
	 * This property may be used by clients wishing to obtain or track the
	 * provisioning agent for the currently running system. When the value of
	 * this property is <code>"true"</code> then the corresponding service is
	 * the agent for the currently running system. If the property is undefined or
	 * has any other value, then the service is not the agent for the currently running system.
	 * </p>
	 */
	public static final String SERVICE_CURRENT = "agent.current"; //$NON-NLS-1$

	/**
	 * Returns the service with the given service name, or <code>null</code>
	 * if no such service is available in this agent.
	 * @exception IllegalStateException if this agent has been stopped
	 */
	public Object getService(String serviceName);

	/**
	 * Registers a service with this provisioning agent.
	 * 
	 * @param serviceName The name of the service to register
	 * @param service The service implementation
	 * @exception IllegalStateException if this agent has been stopped
	 */
	public void registerService(String serviceName, Object service);

	/**
	 * Stops the provisioning agent. This causes services provided by this 
	 * agent to be cleaned up and discarded. No services provided by the agent
	 * should be referenced after the agent has been stopped, and subsequent
	 * attempts to obtain services after the agent has stopped will fail.
	 * <p>
	 * An agent should only be stopped by the client who first created the agent
	 * by invoking {@link IProvisioningAgentProvider#createAgent(java.net.URI)}.
	 * </p>
	 */
	public void stop();

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