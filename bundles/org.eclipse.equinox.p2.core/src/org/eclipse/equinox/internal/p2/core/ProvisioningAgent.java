/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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

import static java.lang.String.format;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;
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
public class ProvisioningAgent implements IProvisioningAgent {

	private final Map<String, Object> agentServices = Collections.synchronizedMap(new LinkedHashMap<>());
	private BundleContext context;
	private volatile boolean stopped = false;
	private ServiceRegistration<IProvisioningAgent> reg;
	private final Map<String, ServiceTracker<IAgentServiceFactory, Object>> trackers = Collections
			.synchronizedMap(new LinkedHashMap<>());

	/**
	 * Instantiates a provisioning agent.
	 */
	public ProvisioningAgent() {
		super();
		registerService(IProvisioningAgent.INSTALLER_AGENT, this);
		registerService(IProvisioningAgent.INSTALLER_PROFILEID, "_SELF_"); //$NON-NLS-1$
	}

	@Override
	public Object getService(String serviceName) {
		//synchronize so concurrent gets always obtain the same service
		synchronized (agentServices) {
			Object service = agentServices.get(serviceName);
			if (service != null) {
				return service;
			}
		}
		ServiceTracker<IAgentServiceFactory, Object> tracker = trackers.computeIfAbsent(serviceName,
				ref -> {
					try {
						if (stopped) {
							return null;
						}
						Filter filter = context.createFilter(
								String.format("(&(%s=%s)(|(%s=%s)(p2.agent.servicename=%s)))", //$NON-NLS-1$
										Constants.OBJECTCLASS, IAgentServiceFactory.class.getName(), //
										IAgentServiceFactory.PROP_AGENT_SERVICE_NAME, serviceName, //
										serviceName)); // use old property as fallback
						return new ServiceTracker<>(context, filter, trackerCustomizer);
					} catch (InvalidSyntaxException e) {
						throw new AssertionError(e);
					}
				});
		if (tracker == null) {
			return null;
		}
		tracker.open();
		return tracker.getService();
	}

	private void checkRunning() {
		if (stopped) {
			throw new IllegalStateException("Attempt to access stopped agent: " + this); //$NON-NLS-1$
		}
	}

	@Override
	public void registerService(String serviceName, Object service) {
		checkRunning();
		if (service instanceof IAgentService) {
			((IAgentService) service).start();
		}
		if (agentServices.put(serviceName, service) instanceof IAgentService prevService) {
			if (prevService == service) {
				agentServices.remove(serviceName, prevService);
				prevService.stop();
				throw new IllegalStateException(format(
						"Service %s for name %s has been registered twice. Double activation of service has happened, service is uregistered.", //$NON-NLS-1$
						service.getClass().getName(), serviceName));
			}
			prevService.stop();
		}
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

	@Override
	public void unregisterService(String serviceName, Object service) {
		agentServices.remove(serviceName, service);
		if (service instanceof IAgentService) {
			((IAgentService) service).stop();
		}
	}

	@Override
	public void stop() {
		stopped = true;
		try {
			disposeInTurns(agentServices.entrySet(), entry -> {
				unregisterService(entry.getKey(), entry.getValue());
			});
		} finally {
			try {
				disposeInTurns(trackers.values(), ServiceTracker::close);
			} finally {
				if (reg != null) {
					reg.unregister();
					reg = null;
				}
			}
		}
	}

	public void setServiceRegistration(ServiceRegistration<IProvisioningAgent> reg) {
		this.reg = reg;
	}

	private final ServiceTrackerCustomizer<IAgentServiceFactory, Object> trackerCustomizer = new ServiceTrackerCustomizer<>() {
		@Override
		public Object addingService(ServiceReference<IAgentServiceFactory> reference) {
			if (stopped) {
				return null;
			}
			Object result = context.getService(reference).createService(ProvisioningAgent.this);
			if (result instanceof IAgentService agentService) {
				agentService.start();
			}
			return result;
		}

		@Override
		public void modifiedService(ServiceReference<IAgentServiceFactory> reference, Object service) {
			// nothing to do
		}

		@Override
		public void removedService(ServiceReference<IAgentServiceFactory> reference, Object service) {
			if (service instanceof IAgentService agentService) {
				agentService.stop();
			}
			if (service != null) {
				context.ungetService(reference);
			}
		}

	};

	/**
	 * Given a thread-safe collection, drain all elements one-by-one, disposing each
	 * without holding locks and allowing concurrent modification form. Other
	 * threads should not attempt to add elements to collection.<br>
	 * Usage example:
	 *
	 * <pre>{@code
	 * volatile boolean stopped = false;
	 * ...
	 * stopped = true; // prevent other threads from adding to the collection
	 * disposeInTurns(agentServices.entrySet(), entry -> {
	 * 	unregisterService(entry.getKey(), entry.getValue());
	 * });
	 * }</pre>
	 *
	 */
	@SuppressWarnings("unchecked")
	private <T> void disposeInTurns(Collection<T> input, Consumer<T> dispose) {
		RuntimeException error = null;
		while (!input.isEmpty()) {
			Object[] toStop = input.toArray(Object[]::new);
			Collections.reverse(Arrays.asList(toStop)); // Remove in an inverse order of adding
			for (Object item : toStop) {
				if (input.remove(item)) {
					try {
						dispose.accept((T) item);
					} catch (RuntimeException e) {
						if (error == null) {
							error = e;
						} else {
							error.addSuppressed(e);
						}
					}
				}
			}
		}
		if (error != null) {
			throw error;
		}
	}

}
