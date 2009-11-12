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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

public class P2Util {
	// XXX Check for updates to this application and return true if
	// we have installed updates and need a restart.
	// This method is intentionally long and ugly in order to provide
	// "one-stop-shopping" for how to check for and perform an update.
	static boolean checkForUpdates() {
		// Before we show a progress dialog, at least find out that we have
		// installed content and repos to check.
		final IProvisioningAgent agent = (IProvisioningAgent) ServiceHelper
				.getService(Activator.bundleContext,
						IProvisioningAgent.SERVICE_NAME);
		if (agent == null)
			return false;
		ProvisioningSession session = new ProvisioningSession(agent);
		IProfile profile = session.getProfile(IProfileRegistry.SELF);
		if (profile == null)
			return false;

		// We are going to look for updates to all IU's in the profile. A
		// different query could be used if we are looking for updates to
		// a subset. For example, the p2 UI only looks for updates to those
		// IU's marked with a special property.
		Collector collector = profile.query(InstallableUnitQuery.ANY,
				new Collector(), null);
		if (collector.isEmpty())
			return false;
		IInstallableUnit[] installed = (IInstallableUnit[]) collector
				.toArray(IInstallableUnit.class);
		final UpdateOperation operation = new UpdateOperation(session,
				installed);
		
		final boolean [] didWeUpdate = new boolean [] {true};

		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				SubMonitor sub = SubMonitor.convert(monitor,
						"Checking for application updates...", 200);
				IStatus status = operation.resolveModal(sub.newChild(100));
				if (status.getSeverity() == IStatus.CANCEL)
					throw new InterruptedException();
				if (status.getSeverity() != IStatus.ERROR) {
					ProvisioningJob job = operation.getProvisioningJob(null);
					status = job.runModal(sub.newChild(100));
					if (status.getSeverity() == IStatus.CANCEL)
						throw new InterruptedException();
					if (status.getSeverity() != IStatus.ERROR) {
						didWeUpdate[0] = false;
					}
				}
			}
		};
		try {
			new ProgressMonitorDialog(null).run(true, true, runnable);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return didWeUpdate[0];
	}
}
