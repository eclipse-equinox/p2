/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.examples.rcp.prestartupdate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UpdateOperation;

public class P2Util {
	// XXX Check for updates to this application and return true if
	// we have installed updates and need a restart.
	static boolean checkForUpdates(IProgressMonitor monitor) {
		IProvisioningAgent agent = (IProvisioningAgent) ServiceHelper
				.getService(Activator.bundleContext,
						IProvisioningAgent.SERVICE_NAME);
		if (agent == null)
			return false;
		ProvisioningSession session = new ProvisioningSession(agent);
		// the default update operation looks for updates to the currently
		// running profile, using the default profile root marker. To change
		// which installable units are being updated, use the more detailed
		// constructors.
		UpdateOperation operation = new UpdateOperation(session);

		SubMonitor sub = SubMonitor.convert(monitor,
				"Checking for application updates...", 200);
		IStatus status = operation.resolveModal(sub.newChild(100));
		if (status.getSeverity() == IStatus.CANCEL)
			throw new OperationCanceledException();
		if (status.getSeverity() != IStatus.ERROR) {
			ProvisioningJob job = operation.getProvisioningJob(null);
			status = job.runModal(sub.newChild(100));
			if (status.getSeverity() == IStatus.CANCEL)
				throw new OperationCanceledException();
			if (status.getSeverity() != IStatus.ERROR) {
				return true;
			}
		}
		return false;
	}
}
