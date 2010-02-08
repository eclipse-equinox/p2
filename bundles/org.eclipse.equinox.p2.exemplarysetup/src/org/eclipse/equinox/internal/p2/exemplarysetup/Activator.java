/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith, Inc - Ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.exemplarysetup;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.garbagecollector.GarbageCollector;
import org.eclipse.equinox.p2.core.*;
import org.osgi.framework.*;

public class Activator implements BundleActivator {
	public static BundleContext context;
	public static final String ID = "org.eclipse.equinox.p2.exemplarysetup"; //$NON-NLS-1$

	private IProvisioningAgent agent;

	/**
	 * Register the agent instance representing the currently running system.
	 * This will be the "default" agent for anyone not specifically trying to manipulate
	 * a different p2 agent location
	 */
	private void registerAgent() {
		//currently location is defined by p2.core but will be defined by the agent in the future
		//for now continue to treat it as a singleton
		ServiceReference locationRef = context.getServiceReference(IAgentLocation.SERVICE_NAME);
		if (locationRef == null)
			throw new RuntimeException("Unable to instantiate p2 agent because agent location is not available"); //$NON-NLS-1$
		IAgentLocation location = (IAgentLocation) context.getService(locationRef);
		if (location == null)
			throw new RuntimeException("Unable to instantiate p2 agent because agent location is not available"); //$NON-NLS-1$

		ServiceReference agentProviderRef = context.getServiceReference(IProvisioningAgentProvider.SERVICE_NAME);
		IProvisioningAgentProvider provider = (IProvisioningAgentProvider) context.getService(agentProviderRef);
		try {
			agent = provider.createAgent(null);
		} catch (Exception e) {
			//we can't proceed without an agent, so fail early
			final String msg = "Unable to instantiate p2 agent at location " + location.getRootLocation(); //$NON-NLS-1$
			LogHelper.log(new Status(IStatus.ERROR, ID, msg, e));
			throw new RuntimeException(msg);
		}

	}

	public void start(BundleContext aContext) throws Exception {
		//Need to do the configuration of all the bits and pieces:
		Activator.context = aContext;
		registerAgent();

		startGarbageCollector();

		//create artifact repositories
		//		registerDefaultArtifactRepoManager();
	}

	private void startGarbageCollector() {
		new GarbageCollector();
	}

	public void stop(BundleContext aContext) throws Exception {
		unregisterAgent();
		Activator.context = null;
	}

	private void unregisterAgent() {
		if (agent != null) {
			agent.stop();
			agent = null;
		}
	}

}
