/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.provisional.p2.engine.IEngine;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class EngineActivator implements BundleActivator, ServiceTrackerCustomizer {
	private static BundleContext context;
	public static final String ID = "org.eclipse.equinox.p2.engine"; //$NON-NLS-1$

	/**
	 * System property describing the profile registry file format
	 */
	public static final String PROP_PROFILE_FORMAT = "eclipse.p2.profileFormat"; //$NON-NLS-1$

	/**
	 * Value for the PROP_PROFILE_FORMAT system property specifying raw XML file
	 * format (used in p2 until and including 3.5.0 release).
	 */
	public static final String PROFILE_FORMAT_UNCOMPRESSED = "uncompressed"; //$NON-NLS-1$

	/**
	 * System property specifying how the engine should handle unsigned artifacts.
	 * If this property is undefined, the default value is assumed to be "prompt".
	 */
	public static final String PROP_UNSIGNED_POLICY = "eclipse.p2.unsignedPolicy"; //$NON-NLS-1$

	/**
	 * System property value specifying that the engine should prompt for confirmation
	 * when installing unsigned artifacts.
	 */
	public static final String UNSIGNED_PROMPT = "prompt"; //$NON-NLS-1$

	/**
	 * System property value specifying that the engine should fail when an attempt
	 * is made to install unsigned artifacts.
	 */
	public static final String UNSIGNED_FAIL = "fail"; //$NON-NLS-1$

	/**
	 * System property value specifying that the engine should silently allow unsigned
	 * artifacts to be installed.
	 */
	public static final String UNSIGNED_ALLOW = "allow"; //$NON-NLS-1$

	private ServiceRegistration registration;

	private ServiceTracker tracker;

	public static BundleContext getContext() {
		return context;
	}

	public Object addingService(ServiceReference reference) {
		if (registration == null) {
			//TODO: eventually we shouldn't register a singleton engine automatically
			IProvisioningAgent agent = (IProvisioningAgent) context.getService(reference);
			IEngine engine = (IEngine) agent.getService(IEngine.SERVICE_NAME);
			registration = context.registerService(IEngine.SERVICE_NAME, engine, null);
			return agent;
		}
		return null;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// nothing to do
	}

	public void removedService(ServiceReference reference, Object service) {
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
	}

	public void start(BundleContext aContext) throws Exception {
		EngineActivator.context = aContext;
		//only want to register a service for the agent of the currently running system
		String filter = "(&(objectClass=" + IProvisioningAgent.SERVICE_NAME + ")(agent.current=true))"; //$NON-NLS-1$ //$NON-NLS-2$
		tracker = new ServiceTracker(context, aContext.createFilter(filter), this);
		tracker.open();
	}

	public void stop(BundleContext aContext) throws Exception {
		tracker.close();
		tracker = null;
		//ensure there are no more profile preference save jobs running
		Job.getJobManager().join(ProfilePreferences.PROFILE_SAVE_JOB_FAMILY, null);

		EngineActivator.context = null;
	}

}
