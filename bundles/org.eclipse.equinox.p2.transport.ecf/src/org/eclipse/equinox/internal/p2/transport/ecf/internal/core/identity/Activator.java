/****************************************************************************
 * Copyright (c) 2004, 2026 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.equinox.internal.p2.transport.ecf.internal.core.identity;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.transport.ecf.core.util.*;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator implements BundleActivator {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.ecf.identity"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private BundleContext context;

	@SuppressWarnings("rawtypes")
	private ServiceTracker debugOptionsTracker;

	@SuppressWarnings("rawtypes")
	private ServiceTracker logServiceTracker;

	private LogService logService;

	public Activator() {
		// public null constructor
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public synchronized DebugOptions getDebugOptions() {
		if (context == null)
			return null;
		if (debugOptionsTracker == null) {
			debugOptionsTracker = new ServiceTracker(context, DebugOptions.class.getName(), null);
			debugOptionsTracker.open();
		}
		return (DebugOptions) debugOptionsTracker.getService();
	}

	@Override
	public void start(BundleContext ctxt) throws Exception {
		plugin = this;
		this.context = ctxt;
		BundleStarter.startDependents(this.context, new String[] { "org.eclipse.equinox.common",
				"org.eclipse.core.jobs", "org.eclipse.equinox.concurrent" }, Bundle.STARTING | Bundle.RESOLVED);
	}

	public BundleContext getBundleContext() {
		return context;
	}

	Bundle getBundle() {
		if (context == null)
			return null;
		return context.getBundle();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	synchronized LogService getLogService() {
		if (context == null) {
			if (logService == null)
				logService = new SystemLogService(PLUGIN_ID);
			return logService;
		}
		if (logServiceTracker == null) {
			logServiceTracker = new ServiceTracker(this.context, LogService.class.getName(), null);
			logServiceTracker.open();
		}
		logService = (LogService) logServiceTracker.getService();
		if (logService == null)
			logService = new SystemLogService(PLUGIN_ID);
		return logService;
	}

	public void log(IStatus status) {
		if (logService == null)
			logService = getLogService();

		if (logService != null)
			logService.log(LogHelper.getLogCode(status), LogHelper.getLogMessage(status), status.getException());
	}

	@Override
	public void stop(BundleContext ctxt) throws Exception {
		if (logServiceTracker != null) {
			logServiceTracker.close();
			logServiceTracker = null;
			logService = null;
		}
		if (debugOptionsTracker != null) {
			debugOptionsTracker.close();
			debugOptionsTracker = null;
		}
		context = null;
		plugin = null;
	}

	public synchronized static Activator getDefault() {
		if (plugin == null)
			plugin = new Activator();
		return plugin;
	}

}
