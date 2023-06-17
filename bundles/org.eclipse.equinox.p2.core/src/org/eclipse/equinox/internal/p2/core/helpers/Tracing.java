/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core.helpers;

import java.util.Date;
import org.eclipse.equinox.internal.p2.core.Activator;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.*;

/**
 * Manages debug tracing options and provides convenience methods for printing
 * debug statements.
 */
public class Tracing {
	//master p2 debug flag
	public static boolean DEBUG = false;

	//debug constants
	public static boolean DEBUG_GENERATOR_PARSING = false;
	public static boolean DEBUG_INSTALL_REGISTRY = false;
	public static boolean DEBUG_METADATA_PARSING = false;
	public static boolean DEBUG_MIRRORS = false;
	public static boolean DEBUG_PARSE_PROBLEMS = false;
	public static boolean DEBUG_PLANNER_OPERANDS = false;
	public static boolean DEBUG_PLANNER_PROJECTOR = false;
	public static boolean DEBUG_PLANNER_PROJECTOR_ENCODING = false;
	public static boolean DEBUG_PROFILE_PREFERENCES = false;
	public static boolean DEBUG_PUBLISHING = false;
	public static boolean DEBUG_RECONCILER = false;
	public static boolean DEBUG_REMOVE_REPO = false;
	public static boolean DEBUG_UPDATE_CHECK = false;
	public static boolean DEBUG_EVENTS_CLIENT = false;
	public static boolean DEBUG_DEFAULT_UI = false;

	static {
		Bundle bundle = FrameworkUtil.getBundle(Tracing.class);
		if (bundle != null) {
			BundleContext bundleContext = bundle.getBundleContext();
			DebugOptions options = ServiceHelper.getService(bundleContext, DebugOptions.class);
			if (options != null) {
				DEBUG = options.getBooleanOption(Activator.ID + "/debug", false); //$NON-NLS-1$
				if (DEBUG) {
					DEBUG_EVENTS_CLIENT = options.getBooleanOption(Activator.ID + "/events/client", false); //$NON-NLS-1$
					DEBUG_GENERATOR_PARSING = options.getBooleanOption(Activator.ID + "/generator/parsing", false); //$NON-NLS-1$
					DEBUG_INSTALL_REGISTRY = options.getBooleanOption(Activator.ID + "/engine/installregistry", false); //$NON-NLS-1$
					DEBUG_METADATA_PARSING = options.getBooleanOption(Activator.ID + "/metadata/parsing", false); //$NON-NLS-1$
					DEBUG_MIRRORS = options.getBooleanOption(Activator.ID + "/artifacts/mirrors", false); //$NON-NLS-1$
					DEBUG_PARSE_PROBLEMS = options.getBooleanOption(Activator.ID + "/core/parseproblems", false); //$NON-NLS-1$
					DEBUG_PLANNER_OPERANDS = options.getBooleanOption(Activator.ID + "/planner/operands", false); //$NON-NLS-1$
					DEBUG_PLANNER_PROJECTOR = options.getBooleanOption(Activator.ID + "/planner/projector", false); //$NON-NLS-1$
					DEBUG_PLANNER_PROJECTOR_ENCODING = options.getBooleanOption(Activator.ID + "/planner/encoding", //$NON-NLS-1$
							false);
					DEBUG_PROFILE_PREFERENCES = options.getBooleanOption(Activator.ID + "/engine/profilepreferences", //$NON-NLS-1$
							false);
					DEBUG_PUBLISHING = options.getBooleanOption(Activator.ID + "/publisher", false); //$NON-NLS-1$
					DEBUG_RECONCILER = options.getBooleanOption(Activator.ID + "/reconciler", false); //$NON-NLS-1$
					DEBUG_REMOVE_REPO = options.getBooleanOption(Activator.ID + "/core/removeRepo", false); //$NON-NLS-1$
					DEBUG_UPDATE_CHECK = options.getBooleanOption(Activator.ID + "/updatechecker", false); //$NON-NLS-1$
					DEBUG_DEFAULT_UI = options.getBooleanOption(Activator.ID + "/ui/default", false); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Prints a debug message on stdout. Callers should first ensure their specific
	 * debug option is enabled.
	 */
	public static void debug(String message) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("[p2] "); //$NON-NLS-1$
		buffer.append(new Date(System.currentTimeMillis()));
		buffer.append(" - ["); //$NON-NLS-1$
		buffer.append(Thread.currentThread().getName());
		buffer.append("] "); //$NON-NLS-1$
		buffer.append(message);
		System.out.println(buffer.toString());
	}
}
