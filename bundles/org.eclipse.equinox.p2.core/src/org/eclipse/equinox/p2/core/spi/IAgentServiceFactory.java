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
	public static final String SERVICE_NAME = IAgentServiceFactory.class.getName();

	/**
	 * The service property specifying the name of the service created by this factory.
	 */
	public static final String PROP_CREATED_SERVICE_NAME = "p2.agent.servicename"; //$NON-NLS-1$

	/**
	 * Instantiates a service instance for the given provisioning agent.
	 * 
	 * @param agent The agent this service will belong to
	 * @return The created service
	 */
	public Object createService(IProvisioningAgent agent);
}
