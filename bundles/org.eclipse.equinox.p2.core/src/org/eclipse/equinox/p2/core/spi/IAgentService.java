/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.core.spi;

import org.eclipse.equinox.p2.core.IProvisioningAgent;

/**
 * Services created by {@link IAgentServiceFactory} objects can optionally implement
 * this interface to participate in the agent lifecycle.
 * @since 2.0
 */
public interface IAgentService {
	/**
	 * This method is invoked when a service is added to an agent. This can occur
	 * either because a client looked up the service and it was lazily instantiated by
	 * the agent, or because the service was registered manually via {@link IProvisioningAgent#registerService(String, Object)}.
	 */
	public void start();

	/**
	 * This method is invoked when a service is removed from an agent. This can occur
	 * either because the agent was stopped, or because the service was manually
	 * unregistered via {@link IProvisioningAgent#unregisterService(String, Object)}.
	 * <p>
	 * Services must not attempt to obtain further services from their agent while
	 * stopping, as some required services may no longer be available.
	 */
	public void stop();

}
