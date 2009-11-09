/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core;

import org.eclipse.equinox.p2.core.IAgentLocation;

import java.net.URI;
import java.util.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.osgi.framework.*;

/**
 * Represents a p2 agent instance.
 */
public class ProvisioningAgent implements IProvisioningAgent {

	private final Map agentServices = Collections.synchronizedMap(new HashMap());

	private BundleContext context;

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.core.IProvisioningAgent#getService(java.lang.String)
	 */
	public Object getService(String serviceName) {
		Object service = agentServices.get(serviceName);
		if (service != null)
			return service;
		//attempt to get factory service from service registry
		ServiceReference[] refs;
		try {
			refs = context.getServiceReferences(IAgentServiceFactory.SERVICE_NAME, "(" + IAgentServiceFactory.PROP_CREATED_SERVICE_NAME + '=' + serviceName + ')'); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
			return null;
		}
		if (refs == null || refs.length == 0)
			return null;
		IAgentServiceFactory factory = (IAgentServiceFactory) context.getService(refs[0]);
		if (factory == null)
			return null;
		try {
			service = factory.createService(this);
		} finally {
			context.ungetService(refs[0]);
		}
		if (service != null)
			agentServices.put(serviceName, service);
		return service;
	}

	public void registerService(String serviceName, Object service) {
		agentServices.put(serviceName, service);
	}

	public void setBundleContext(BundleContext context) {
		this.context = context;
	}

	public void setLocation(URI location) {
		//treat a null location as using the currently running platform
		IAgentLocation agentLocation = null;
		if (location == null) {
			ServiceReference ref = context.getServiceReference(IAgentLocation.SERVICE_NAME);
			if (ref != null) {
				agentLocation = (IAgentLocation) context.getService(ref);
				context.ungetService(ref);
			}
		} else {
			agentLocation = new AgentLocation(location);
		}
		agentServices.put(IAgentLocation.SERVICE_NAME, agentLocation);
	}

	public void unregisterService(String serviceName, Object service) {
		synchronized (agentServices) {
			if (agentServices.get(serviceName) == service)
				agentServices.remove(serviceName);
		}
	}
}
