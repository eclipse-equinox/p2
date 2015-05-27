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
package org.eclipse.equinox.internal.p2.core;

import java.net.URI;
import java.util.*;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentService;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Represents a p2 agent instance.
 */
public class ProvisioningAgent implements IProvisioningAgent, ServiceTrackerCustomizer<IAgentServiceFactory, Object> {

	private final Map<String, Object> agentServices = Collections.synchronizedMap(new HashMap<String, Object>());
	private BundleContext context;
	private volatile boolean stopped = false;
	private ServiceRegistration<IProvisioningAgent> reg;
	private final Map<ServiceReference<IAgentServiceFactory>, ServiceTracker<IAgentServiceFactory, Object>> trackers = Collections.synchronizedMap(new HashMap<ServiceReference<IAgentServiceFactory>, ServiceTracker<IAgentServiceFactory, Object>>());

	/**
	 * Instantiates a provisioning agent.
	 */
	public ProvisioningAgent() {
		super();
		registerService(IProvisioningAgent.INSTALLER_AGENT, this);
		registerService(IProvisioningAgent.INSTALLER_PROFILEID, "_SELF_"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.core.IProvisioningAgent#getService(java.lang.String)
	 */
	public Object getService(String serviceName) {
		//synchronize so concurrent gets always obtain the same service
		synchronized (agentServices) {
			checkRunning();
			Object service = agentServices.get(serviceName);
			if (service != null)
				return service;
			//attempt to get factory service from service registry
			Collection<ServiceReference<IAgentServiceFactory>> refs;
			try {
				refs = context.getServiceReferences(IAgentServiceFactory.class, "(" + IAgentServiceFactory.PROP_CREATED_SERVICE_NAME + '=' + serviceName + ')'); //$NON-NLS-1$
			} catch (InvalidSyntaxException e) {
				e.printStackTrace();
				return null;
			}
			if (refs == null || refs.isEmpty())
				return null;
			ServiceReference<IAgentServiceFactory> firstRef = Collections.max(refs);
			//track the factory so that we can automatically remove the service when the factory goes away
			ServiceTracker<IAgentServiceFactory, Object> tracker = new ServiceTracker<IAgentServiceFactory, Object>(context, firstRef, this);
			tracker.open();
			IAgentServiceFactory factory = (IAgentServiceFactory) tracker.getService();
			if (factory == null) {
				tracker.close();
				return null;
			}
			service = factory.createService(this);
			if (service == null) {
				tracker.close();
				return null;
			}
			registerService(serviceName, service);
			trackers.put(firstRef, tracker);
			return service;
		}
	}

	private void checkRunning() {
		if (stopped)
			throw new IllegalStateException("Attempt to access stopped agent: " + this); //$NON-NLS-1$
	}

	public void registerService(String serviceName, Object service) {
		checkRunning();
		agentServices.put(serviceName, service);
		if (service instanceof IAgentService)
			((IAgentService) service).start();
	}

	public void setBundleContext(BundleContext context) {
		this.context = context;
	}

	public void setLocation(URI location) {
		//treat a null location as using the currently running platform
		IAgentLocation agentLocation = null;
		if (location == null) {
			ServiceReference<IAgentLocation> ref = context.getServiceReference(IAgentLocation.class);
			if (ref != null) {
				agentLocation = context.getService(ref);
				context.ungetService(ref);
			}
		} else {
			agentLocation = new AgentLocation(location);
		}
		registerService(IAgentLocation.SERVICE_NAME, agentLocation);
	}

	public void unregisterService(String serviceName, Object service) {
		synchronized (agentServices) {
			if (stopped)
				return;
			if (agentServices.get(serviceName) == service)
				agentServices.remove(serviceName);
		}
		if (service instanceof IAgentService)
			((IAgentService) service).stop();
	}

	public void stop() {
		List<Object> toStop;
		synchronized (agentServices) {
			toStop = new ArrayList<Object>(agentServices.values());
		}
		//give services a chance to do their own shutdown
		for (Object service : toStop) {
			if (service instanceof IAgentService)
				if (service != this)
					((IAgentService) service).stop();
		}
		stopped = true;
		//close all service trackers
		synchronized (trackers) {
			for (ServiceTracker<IAgentServiceFactory, Object> t : trackers.values())
				t.close();
			trackers.clear();
		}
		if (reg != null) {
			reg.unregister();
			reg = null;
		}
	}

	public void setServiceRegistration(ServiceRegistration<IProvisioningAgent> reg) {
		this.reg = reg;
	}

	/*(non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
	 */
	public Object addingService(ServiceReference<IAgentServiceFactory> reference) {
		if (stopped)
			return null;
		return context.getService(reference);
	}

	/*(non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	public void modifiedService(ServiceReference<IAgentServiceFactory> reference, Object service) {
		//nothing to do
	}

	/*(non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	public void removedService(ServiceReference<IAgentServiceFactory> reference, Object factoryService) {
		if (stopped)
			return;
		String serviceName = (String) reference.getProperty(IAgentServiceFactory.PROP_CREATED_SERVICE_NAME);
		if (serviceName == null)
			return;
		Object registered = agentServices.get(serviceName);
		if (registered == null)
			return;
		if (FrameworkUtil.getBundle(registered.getClass()) == FrameworkUtil.getBundle(factoryService.getClass())) {
			//the service we are holding is going away
			unregisterService(serviceName, registered);
			ServiceTracker<IAgentServiceFactory, Object> toRemove = trackers.remove(reference);
			if (toRemove != null)
				toRemove.close();
		}
	}
}
