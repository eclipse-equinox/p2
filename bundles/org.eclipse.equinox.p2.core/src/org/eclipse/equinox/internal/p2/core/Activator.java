/*******************************************************************************
 *  Copyright (c) 2007, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB (Pascal Rapicault) - reading preferences from base in shared install
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core;

import java.io.File;
import java.net.*;
import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	public static IAgentLocation agentDataLocation = null;

	public static BundleContext context;
	private static final String DEFAULT_AGENT_LOCATION = "../p2"; //$NON-NLS-1$
	public static final String ID = "org.eclipse.equinox.p2.core"; //$NON-NLS-1$

	private static Activator instance;
	// Data mode constants for user, configuration and data locations.
	private static final String NO_DEFAULT = "@noDefault"; //$NON-NLS-1$
	private static final String NONE = "@none"; //$NON-NLS-1$

	private static final String PROP_AGENT_DATA_AREA = "eclipse.p2.data.area"; //$NON-NLS-1$
	private static final String PROP_CONFIG_DIR = "osgi.configuration.area"; //$NON-NLS-1$
	private static final String PROP_SHARED_CONFIG_DIR = "osgi.sharedConfiguration.area"; //$NON-NLS-1$
	private static final String PROP_USER_DIR = "user.dir"; //$NON-NLS-1$
	private static final String PROP_USER_HOME = "user.home"; //$NON-NLS-1$

	public static final String READ_ONLY_AREA_SUFFIX = ".readOnly"; //$NON-NLS-1$

	private static final String VAR_CONFIG_DIR = "@config.dir"; //$NON-NLS-1$
	private static final String VAR_USER_DIR = "@user.dir"; //$NON-NLS-1$
	private static final String VAR_USER_HOME = "@user.home"; //$NON-NLS-1$

	private IProvisioningAgent agent;
	private ServiceRegistration<IAgentLocation> agentLocationRegistration = null;

	ServiceTracker<FrameworkLog, FrameworkLog> logTracker;

	/**
	 * NOTE: This method is copied from LocationHelper in org.eclipse.osgi
	 * due to access restrictions.
	 */
	private static URI adjustTrailingSlash(URI url, boolean trailingSlash) throws URISyntaxException {
		String file = url.toString();
		if (trailingSlash == (file.endsWith("/"))) //$NON-NLS-1$
			return url;
		file = trailingSlash ? file + "/" : file.substring(0, file.length() - 1); //$NON-NLS-1$
		return new URI(file);
	}

	/**
	 * Builds a URL with the given specification
	 * NOTE: This method is copied from LocationHelper in org.eclipse.osgi
	 * due to access restrictions.
	 * 
	 * @param spec the URL specification
	 * @param trailingSlash flag to indicate a trailing slash on the spec
	 * @return a URL
	 */
	private static URI buildURL(String spec, boolean trailingSlash) {
		if (spec == null)
			return null;
		boolean isFile = spec.startsWith("file:"); //$NON-NLS-1$
		try {
			if (isFile)
				return adjustTrailingSlash(new File(spec.substring(5)).toURI(), trailingSlash);
			//for compatibility only allow non-file URI if it is also a legal URL
			//when given "c:/foo" we want to treat it as a file rather than a URI with protocol "c"
			new URL(spec);
			return new URI(spec);
		} catch (Exception e) {
			// if we failed and it is a file spec, there is nothing more we can do
			// otherwise, try to make the spec into a file URL.
			if (isFile)
				return null;
			try {
				return adjustTrailingSlash(new File(spec).toURI(), trailingSlash);
			} catch (URISyntaxException e1) {
				return null;
			}
		}
	}

	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Returns the framework log, or null if not available
	 */
	public static FrameworkLog getFrameworkLog() {
		//protect against concurrent shutdown
		Activator a = instance;
		if (a == null)
			return null;
		ServiceTracker<FrameworkLog, FrameworkLog> tracker = a.getLogTracker();
		if (tracker == null)
			return null;
		return tracker.getService();
	}

	private static String substituteVar(String source, String var, String prop) {
		String value = Activator.context.getProperty(prop);
		if (value == null)
			value = ""; //$NON-NLS-1$
		return value + source.substring(var.length());
	}

	private IAgentLocation buildLocation(String property, URI defaultLocation, boolean readOnlyDefault, boolean addTrailingSlash) {
		String location = Activator.context.getProperty(property);
		// if the instance location is not set, predict where the workspace will be and 
		// put the instance area inside the workspace meta area.
		if (location == null)
			return new AgentLocation(defaultLocation);
		if (location.equalsIgnoreCase(NONE))
			return null;
		if (location.equalsIgnoreCase(NO_DEFAULT))
			return new AgentLocation(null);
		if (location.startsWith(VAR_USER_HOME)) {
			String base = substituteVar(location, VAR_USER_HOME, PROP_USER_HOME);
			location = new Path(base).toFile().getAbsolutePath();
		} else if (location.startsWith(VAR_USER_DIR)) {
			String base = substituteVar(location, VAR_USER_DIR, PROP_USER_DIR);
			location = new Path(base).toFile().getAbsolutePath();
		} else if (location.startsWith(VAR_CONFIG_DIR)) {
			//note the config dir system property is already a URL
			location = substituteVar(location, VAR_CONFIG_DIR, PROP_CONFIG_DIR);
		}
		URI url = buildURL(location, addTrailingSlash);
		AgentLocation result = null;
		if (url != null) {
			result = new AgentLocation(url);
		}
		return result;
	}

	private ServiceTracker<FrameworkLog, FrameworkLog> getLogTracker() {
		if (logTracker != null)
			return logTracker;
		//lazy init if the bundle has been started
		if (context == null)
			return null;
		logTracker = new ServiceTracker<FrameworkLog, FrameworkLog>(context, FrameworkLog.class, null);
		logTracker.open();
		return logTracker;
	}

	/**
	 * Register the agent instance representing the currently running system.
	 * This will be the "default" agent for anyone not specifically trying to manipulate
	 * a different p2 agent location
	 */
	private void registerAgent() {
		//no need to register an agent if there is no agent location
		if (agentDataLocation == null)
			return;
		ServiceReference<IProvisioningAgentProvider> agentProviderRef = context.getServiceReference(IProvisioningAgentProvider.class);
		IProvisioningAgentProvider provider = null;
		if (agentProviderRef != null)
			provider = context.getService(agentProviderRef);

		if (provider == null) {
			// If we don't have a provider, which could happen if the p2.core bundle is
			// eagerly started, we should create one.
			provider = new DefaultAgentProvider();
			((DefaultAgentProvider) provider).activate(context);
		}

		try {
			agent = provider.createAgent(null);
		} catch (Exception e) {
			final String msg = "Unable to instantiate p2 agent at location " + agentDataLocation.getRootLocation(); //$NON-NLS-1$
			LogHelper.log(new Status(IStatus.ERROR, ID, msg, e));
		}
		registerSharedAgent(agent, provider);
	}

	private URI computeLocationSharedAgent(IProvisioningAgent currentAgent, IProvisioningAgentProvider provider) {
		//This figures out if we are running in shared mode and computes the location of the p2 folder in the base.
		//Note that this logic only works for the case where the p2 location is colocated with the configuration area
		//(configuration and p2 are sibling of each others).
		//To make that work for other scenarios, the config.ini of the base would have to be read and interpreted. 
		URI location = null;
		String sharedConfigArea = null;
		try {
			sharedConfigArea = context.getProperty(PROP_SHARED_CONFIG_DIR);
			if (sharedConfigArea == null)
				return null;

			//Make sure the property has a trai
			if (!sharedConfigArea.endsWith("/") && !sharedConfigArea.endsWith("\\")) //$NON-NLS-1$ //$NON-NLS-2$
				sharedConfigArea += "/"; //$NON-NLS-1$
			location = URIUtil.fromString(sharedConfigArea + DEFAULT_AGENT_LOCATION + '/');
		} catch (URISyntaxException e) {
			final String msg = "Unable to instantiate p2 agent for shared location " + sharedConfigArea; //$NON-NLS-1$
			LogHelper.log(new Status(IStatus.WARNING, ID, msg, e));
			return null;
		}
		return location;
	}

	private void registerSharedAgent(IProvisioningAgent currentAgent, IProvisioningAgentProvider provider) {
		URI location = computeLocationSharedAgent(currentAgent, provider);
		if (location == null) {
			return;
		}

		IProvisioningAgent sharedAgent;
		try {
			sharedAgent = provider.createAgent(location);
			currentAgent.registerService(IProvisioningAgent.SHARED_BASE_AGENT, sharedAgent);
			sharedAgent.registerService(IProvisioningAgent.SHARED_CURRENT_AGENT, currentAgent);
		} catch (ProvisionException e) {
			final String msg = "Unable to instantiate p2 agent for shared location " + location; //$NON-NLS-1$
			LogHelper.log(new Status(IStatus.WARNING, ID, msg, e));
		}
	}

	public void start(BundleContext aContext) throws Exception {
		instance = this;
		Activator.context = aContext;
		URI defaultLocation = URIUtil.fromString(aContext.getProperty(PROP_CONFIG_DIR) + DEFAULT_AGENT_LOCATION + '/');
		agentDataLocation = buildLocation(PROP_AGENT_DATA_AREA, defaultLocation, false, true);
		Dictionary<String, Object> locationProperties = new Hashtable<String, Object>();
		if (agentDataLocation != null) {
			locationProperties.put("type", PROP_AGENT_DATA_AREA); //$NON-NLS-1$
			agentLocationRegistration = aContext.registerService(IAgentLocation.class, agentDataLocation, locationProperties);
		}
		registerAgent();
	}

	public void stop(BundleContext aContext) throws Exception {
		unregisterAgent();
		instance = null;
		agentDataLocation = null;
		if (agentLocationRegistration != null)
			agentLocationRegistration.unregister();
		if (logTracker != null) {
			logTracker.close();
			logTracker = null;
		}
		Activator.context = null;
	}

	private void unregisterAgent() {
		if (agent != null) {
			agent.stop();
			agent = null;
		}
	}
}
