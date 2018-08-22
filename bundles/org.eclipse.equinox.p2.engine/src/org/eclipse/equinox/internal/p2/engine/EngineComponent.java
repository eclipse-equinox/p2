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

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.engine.IEngine;

/**
 * Component that provides a factory that can create and initialize
 * {@link IEngine} instances.
 */
public class EngineComponent implements IAgentServiceFactory {

	@Override
	public Object createService(IProvisioningAgent agent) {
		//ensure there is a garbage collector created for this agent if available
		agent.getService("org.eclipse.equinox.internal.p2.garbagecollector.GarbageCollector"); //$NON-NLS-1$
		//various parts of the engine may need an open-ended set of services, so we pass the agent to the engine directly
		return new Engine(agent);
	}
}
