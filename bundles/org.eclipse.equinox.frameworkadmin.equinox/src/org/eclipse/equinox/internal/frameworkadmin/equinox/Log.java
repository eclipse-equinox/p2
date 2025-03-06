/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

package org.eclipse.equinox.internal.frameworkadmin.equinox;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Utility class with static methods for logging to LogService, if available
 */
@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
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

	static void debug(Object obj, String method, String message) {
		log(LogService.LOG_DEBUG, obj, method, message, null);
	}

	static void warn(Object obj, String method, String message) {
		log(LogService.LOG_WARNING, obj, method, message, null);
	}

	static void info(Object obj, String method, String message) {
		log(LogService.LOG_INFO, obj, method, message, null);
	}

	private static void log(int level, Object obj, String method, String message, Throwable e) {
		LogService logService = null;
		String msg = ""; //$NON-NLS-1$
		if (method == null) {
			if (obj != null) {
				msg = "(" + obj.getClass().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else if (obj == null) {
			msg = "[" + method + "]" + message; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			msg = "[" + method + "](" + obj.getClass().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		msg += message;
		if (logTracker != null) {
			logService = (LogService) logTracker.getService();
		}

		if (logService != null) {
			logService.log(level, msg, e);
		} else {
			String levelSt = null;
			if (level == LogService.LOG_DEBUG) {
				levelSt = "DEBUG"; //$NON-NLS-1$
			} else if (level == LogService.LOG_INFO) {
				levelSt = "INFO"; //$NON-NLS-1$
			} else if (level == LogService.LOG_WARNING) {
				levelSt = "WARNING"; //$NON-NLS-1$
			} else if (level == LogService.LOG_ERROR) {
				levelSt = "ERROR"; //$NON-NLS-1$
				useLog = true;
			}
			if (useLog) {
				System.err.println("[" + levelSt + "]" + msg); //$NON-NLS-1$ //$NON-NLS-2$
				if (e != null) {
					e.printStackTrace();
				}
			}
		}
	}

	static void warn(Object obj, String method, Throwable e) {
		log(LogService.LOG_WARNING, obj, method, null, e);
	}

	static void debug(String message) {
		log(LogService.LOG_DEBUG, null, null, message, null);
	}

	static void warn(String message) {
		log(LogService.LOG_WARNING, null, null, message, null);
	}

	static void info(String message) {
		log(LogService.LOG_INFO, null, null, message, null);
	}

	static void error(String message) {
		log(LogService.LOG_ERROR, null, null, message, null);
	}

	static void error(String message, Throwable e) {
		log(LogService.LOG_ERROR, null, null, message, e);
	}

	private Log() {
	}

}
