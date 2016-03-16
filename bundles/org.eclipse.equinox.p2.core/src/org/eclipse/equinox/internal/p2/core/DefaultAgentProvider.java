/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core;

import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.equinox.p2.core.*;
import org.osgi.framework.*;
import org.osgi.framework.Constants;

/**
 * Default implementation of {@link IProvisioningAgentProvider}.
 */
public class DefaultAgentProvider implements IProvisioningAgentProvider {
	private BundleContext context;

	public void activate(BundleContext aContext) {
		this.context = aContext;
	}

	public IProvisioningAgent createAgent(URI location) {
		ProvisioningAgent result = new ProvisioningAgent();
		result.setBundleContext(context);
		result.setLocation(location);
		IAgentLocation agentLocation = (IAgentLocation) result.getService(IAgentLocation.SERVICE_NAME);
		Dictionary<String, Object> properties = new Hashtable<String, Object>(5);
		if (agentLocation != null)
			properties.put("locationURI", String.valueOf(agentLocation.getRootLocation())); //$NON-NLS-1$
		//make the currently running system have a higher service ranking
		if (location == null) {
			properties.put(Constants.SERVICE_RANKING, Integer.valueOf(100));
			properties.put(IProvisioningAgent.SERVICE_CURRENT, Boolean.TRUE.toString());
		}
		ServiceRegistration<IProvisioningAgent> reg = context.registerService(IProvisioningAgent.class, result, properties);
		result.setServiceRegistration(reg);
		return result;
	}
}
