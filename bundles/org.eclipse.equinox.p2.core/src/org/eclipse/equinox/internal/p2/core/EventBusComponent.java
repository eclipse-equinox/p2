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
package org.eclipse.equinox.internal.p2.core;

import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.osgi.service.component.annotations.Component;

/**
 * Factory for creating {@link IProvisioningEventBus} instances.
 */
@Component(service = IAgentServiceFactory.class, property = IAgentServiceFactory.PROP_CREATED_SERVICE_NAME + "="
		+ IProvisioningEventBus.SERVICE_NAME, name = "org.eclipse.equinox.p2.core.eventbus")
public class EventBusComponent implements IAgentServiceFactory {
	@Override
	public Object createService(IProvisioningAgent agent) {
		return new ProvisioningEventBus();
	}

}
