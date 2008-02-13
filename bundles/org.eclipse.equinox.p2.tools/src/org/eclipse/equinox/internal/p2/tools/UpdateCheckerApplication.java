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
package org.eclipse.equinox.internal.p2.tools;

import java.util.Map;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.updatechecker.UpdateChecker;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.*;

/**
 * A little app that demonstrates how to register for automatic update checks on
 * a profile.
 * 
 *  -profile profileID (specifies the profile id to check for updates)
 *  -delay long (specifies a long which is the initial delay before beginning polling)
 *  -poll long (specifies a long which is the frequency of the update poll)
 *  -debug, -trace can be used to show you what's going on.
 *  
 *   This checker simply writes to system.out when updates are available and lists the
 *   IU's that have updates available.
 *   
 * @since 3.4
 */
public class UpdateCheckerApplication implements IApplication {

	private static String ARG_PROFILE = "-profile"; //$NON-NLS-1$
	private static String ARG_POLL = "-poll"; //$NON-NLS-1$
	private static String ARG_DELAY = "-delay"; //$NON-NLS-1$
	private static String ARG_DEBUG = "-debug"; //$NON-NLS-1$
	private static String ARG_TRACE = "-trace"; //$NON-NLS-1$
	String profileId;
	long delay = IUpdateChecker.ONE_TIME_CHECK;
	long poll = IUpdateChecker.ONE_TIME_CHECK;
	IUpdateChecker checker;
	IUpdateListener listener = new IUpdateListener() {

		public void updatesAvailable(UpdateEvent event) {
			System.out.println("Updates available for " + profileId); //$NON-NLS-1$
			IInstallableUnit[] ius = event.getIUs();
			for (int i = 0; i < ius.length; i++)
				System.out.println(ius[i].toString());
		}

	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		checker = (IUpdateChecker) ServiceHelper.getService(Activator.getContext(), IUpdateChecker.SERVICE_NAME);
		if (checker == null)
			throw new RuntimeException("Update checker could not be loaded."); //$NON-NLS-1$

		Map args = context.getArguments();
		initializeFromArguments((String[]) args.get("application.args")); //$NON-NLS-1$
		if (profileId == null) {
			System.out.println("Must specify a profile id using -profile arg"); //$NON-NLS-1$
		} else {
			checker.addUpdateCheck(profileId, delay, poll, listener);
		}

		return null;
	}

	public void stop() {
		checker.removeUpdateCheck(listener);
	}

	public void initializeFromArguments(String[] args) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals(ARG_DEBUG))
				UpdateChecker.DEBUG = true;
			else if (args[i].equals(ARG_TRACE))
				UpdateChecker.TRACE = true;

			// The remaining args have parameters.  If we are at the
			// last argument, or if the next one starts with a '-',
			// then there won't be a parm, so skip this one.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;

			String arg = args[++i];

			if (args[i - 1].equalsIgnoreCase(ARG_PROFILE))
				profileId = arg;
			else if (args[i - 1].equalsIgnoreCase(ARG_POLL))
				poll = getLong(ARG_POLL, arg, IUpdateChecker.ONE_TIME_CHECK);
			else if (args[i - 1].equalsIgnoreCase(ARG_DELAY))
				delay = getLong(ARG_DELAY, arg, IUpdateChecker.ONE_TIME_CHECK);

		}
	}

	long getLong(String argName, String value, long defaultValue) {
		if (value != null)
			try {
				return Long.parseLong(value);
			} catch (Exception e) {
				System.out.println("The value for " + argName + "(" + value + ") is not a long."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		return defaultValue;
	}
}
