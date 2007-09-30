/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives;

import org.eclipse.core.runtime.*;

public class Util {

	public static void log(String msg) {
		log(new Status(IStatus.OK, Activator.ID, IStatus.OK, msg, null));
	}

	public static void log(IStatus status) {
		System.out.println(Util.class.getName() + " " + status);
	}

	public static CoreException coreException(String msg) {
		return new CoreException(errorStatus(msg, null));
	}

	public static CoreException coreException(Throwable e, String msg) {
		return new CoreException(errorStatus(msg, e));
	}

	public static IStatus errorStatus(String msg, Throwable e) {
		return new Status(IStatus.ERROR, Activator.ID, IStatus.OK, msg, e);
	}

	/**
	 * Split monitor into n equal sub-monitors and return them.
	 * This calls beginTask on monitor and assigns all its time to the sub-monitors.
	 * monitor may be null.
	 */
	public static IProgressMonitor[] splitProgressMonitor(IProgressMonitor monitor, int n) {
		if (monitor == null || monitor instanceof NullProgressMonitor) {
			monitor = null;
		} else {
			monitor.beginTask("", n);
		}
		IProgressMonitor[] result = new IProgressMonitor[n];
		for (int i = 0; i < n; i += 1) {
			result[i] = createSubProgressMonitor(monitor, 1);
		}
		return result;
	}

	private static IProgressMonitor createSubProgressMonitor(IProgressMonitor monitor, int ticks) {
		if (monitor == null)
			return new NullProgressMonitor();
		else
			return new SubProgressMonitor(monitor, ticks, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
	}

}
