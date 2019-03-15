/*******************************************************************************
 *  Copyright (c) 2010 Sonatype, Inc and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

	@Override
	public Object createService(IProvisioningAgent agent) {
		final IProvisioningEventBus eventBus = (IProvisioningEventBus) agent.getService(IProvisioningEventBus.SERVICE_NAME);
		CacheManager cache = new CacheManager((IAgentLocation) agent.getService(IAgentLocation.SERVICE_NAME), (Transport) agent.getService(Transport.SERVICE_NAME));
		cache.setEventBus(eventBus);
		return cache;
	}

}
