/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.frameworkadmin.equinox;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Utility class with static methods for logging to LogService, if available 
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Log {
	static private ServiceTracker logTracker;
	static private boolean useLog = false;

	public static void dispose() {
		if (logTracker != null) {
			logTracker.close();
		}
		logTracker = null;
	}

	public static void init(BundleContext bc) {
		logTracker = new ServiceTracker(bc, LogService.class.getName(), null);
		logTracker.open();
	}

	public static void log(int level, Object obj, String method, String message) {
		log(level, obj, method, message, null);
	}

	public static void log(int level, Object obj, String method, String message, Throwable e) {
		LogService logService = null;
		String msg = ""; //$NON-NLS-1$
		if (method == null) {
			if (obj != null)
				msg = "(" + obj.getClass().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		} else if (obj == null)
			msg = "[" + method + "]" + message; //$NON-NLS-1$ //$NON-NLS-2$
		else
			msg = "[" + method + "](" + obj.getClass().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		msg += message;
		if (logTracker != null)
			logService = (LogService) logTracker.getService();

		if (logService != null) {
			logService.log(level, msg, e);
		} else {
			String levelSt = null;
			if (level == LogService.LOG_DEBUG)
				levelSt = "DEBUG"; //$NON-NLS-1$
			else if (level == LogService.LOG_INFO)
				levelSt = "INFO"; //$NON-NLS-1$
			else if (level == LogService.LOG_WARNING)
				levelSt = "WARNING"; //$NON-NLS-1$
			else if (level == LogService.LOG_ERROR) {
				levelSt = "ERROR"; //$NON-NLS-1$
				useLog = true;
			}
			if (useLog) {
				System.err.println("[" + levelSt + "]" + msg); //$NON-NLS-1$ //$NON-NLS-2$
				if (e != null)
					e.printStackTrace();
			}
		}
	}

	public static void log(int level, Object obj, String method, Throwable e) {
		log(level, obj, method, null, e);
	}

	public static void log(int level, String message) {
		log(level, null, null, message, null);
	}

	public static void log(int level, String message, Throwable e) {
		log(level, null, null, message, e);
	}

	private Log() {
	}

}
