/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.internal.p2.core.helpers;

import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.Activator;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.*;

public class LogHelper {

	public static void log(IStatus status) {
		if (!doOSGiLog(status)) {
			System.out.println(status.getMessage());
			if (status.getException() != null)
				status.getException().printStackTrace();
		}
	}

	private static boolean doOSGiLog(IStatus status) {
		Bundle bundle = FrameworkUtil.getBundle(LogHelper.class);
		if (bundle == null) {
			return false;
		}
		BundleContext bundleContext = bundle.getBundleContext();
		if (bundleContext == null) {
			return false;
		}
		ServiceReference<FrameworkLog> reference = bundleContext.getServiceReference(FrameworkLog.class);
		if (reference == null) {
			return false;
		}
		FrameworkLog log = bundleContext.getService(reference);
		if (log == null) {
			return false;
		}
		log.log(getLog(status));
		bundleContext.ungetService(reference);
		return true;
	}

	public static void log(ProvisionException exception) {
		log(new Status(exception.getStatus().getSeverity(), Activator.ID, "Provisioning exception", exception)); //$NON-NLS-1$
	}

	/**
	 * Copied from PlatformLogWriter in core runtime.
	 */
	private static FrameworkLogEntry getLog(IStatus status) {
		Throwable t = status.getException();
		ArrayList<FrameworkLogEntry> childlist = new ArrayList<>();

		int stackCode = t instanceof CoreException ? 1 : 0;
		// ensure a substatus inside a CoreException is properly logged
		if (stackCode == 1) {
			IStatus coreStatus = ((CoreException) t).getStatus();
			if (coreStatus != null) {
				childlist.add(getLog(coreStatus));
			}
		}

		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (IStatus child : children) {
				childlist.add(getLog(child));
			}
		}

		FrameworkLogEntry[] children = (childlist.size() == 0 ? null : childlist.toArray(new FrameworkLogEntry[childlist.size()]));

		return new FrameworkLogEntry(status.getPlugin(), status.getSeverity(), status.getCode(), status.getMessage(), stackCode, t, children);
	}
}
