/*******************************************************************************
 *  Copyright (c) 2010 Sonatype, Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;

public class CacheManagerComponent implements IAgentServiceFactory {

	public Object createService(IProvisioningAgent agent) {
		final IProvisioningEventBus eventBus = (IProvisioningEventBus) agent.getService(IProvisioningEventBus.SERVICE_NAME);
		CacheManager cache = new CacheManager((IAgentLocation) agent.getService(IAgentLocation.SERVICE_NAME), (Transport) agent.getService(Transport.SERVICE_NAME));
		cache.setEventBus(eventBus);
		return cache;
	}

}
