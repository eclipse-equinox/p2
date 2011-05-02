/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.core;

import java.net.URI;

/**
 * An OSGi service that is used to create or obtain instances of an
 * {@link IProvisioningAgent}.
 * @since 2.0
 */
public interface IProvisioningAgentProvider {

	/**
	 * Service name constant for the agent provider service.
	 */
	public static final String SERVICE_NAME = IProvisioningAgentProvider.class.getName();

	/**
	 * Creates a provisioning agent who metadata is stored at the given location.
	 * If a <code>null</code> location is provided, the provisioning agent for the 
	 * currently running system is returned, if available. If a <code>null</code>
	 * location is provided and the currently running system has not been provisioned
	 * by any known agent, <code>null</code> is returned.
	 * <p>
	 * Callers of this method are responsible for stopping the agent
	 * when they are finished using it by invoking {@link IProvisioningAgent#stop()}.
	 * </p>
	 * @param location The location where the agent metadata is stored
	 * @return A provisioning agent, or <code>null</code> if a <code>null</code>
	 * parameter is provided and there is no currently running agent.
	 * @throws ProvisionException If agent creation failed. Reasons include:
	 * <ul>
	 * <li>The location is not writeable.</li>
	 * </ul>
	 * @see IProvisioningAgent#stop()
	 */
	public IProvisioningAgent createAgent(URI location) throws ProvisionException;
}
