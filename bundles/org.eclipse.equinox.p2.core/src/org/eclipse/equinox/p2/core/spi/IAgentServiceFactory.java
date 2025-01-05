/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.p2.core.spi;

import org.eclipse.equinox.p2.core.IProvisioningAgent;

/**
 * A factory for creating a service that forms part of a provisioning agent instance.
 * Factories themselves are registered in the OSGi service registry so that they
 * can be obtained by a provisioning agent.
 * @since 2.0
 */
public interface IAgentServiceFactory {
	/**
	 * The service name for the factory service.
	 */
	String SERVICE_NAME = IAgentServiceFactory.class.getName();

	/**
	 * The service property specifying the name of the service created by this
	 * factory.
	 *
	 * @deprecated use {@link #PROP_AGENT_SERVICE_NAME} instead
	 */
	@Deprecated()
	String PROP_CREATED_SERVICE_NAME = "p2.agent.servicename"; //$NON-NLS-1$

	/**
	 * The service property specifying the name of the service created by this
	 * factory.
	 *
	 * @since 2.13
	 */
	String PROP_AGENT_SERVICE_NAME = "p2.agent.service.name"; //$NON-NLS-1$

	/**
	 * Instantiates a service instance for the given provisioning agent.
	 *
	 * @param agent The agent this service will belong to
	 * @return The created service
	 */
	Object createService(IProvisioningAgent agent);
}
