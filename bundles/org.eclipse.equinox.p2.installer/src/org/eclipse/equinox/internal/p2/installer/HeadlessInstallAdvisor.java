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
package org.eclipse.equinox.internal.p2.installer;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.installer.*;

/**
 * A headless install advisor that prints everything to a log.
 */
public class HeadlessInstallAdvisor extends InstallAdvisor {
	class HeadlessProgressMonitor implements IProgressMonitor {
		private boolean canceled;

		public void beginTask(String name, int totalWork) {
			setResult(new Status(IStatus.INFO, InstallerActivator.PI_INSTALLER, name));
		}

		public void done() {
			//nothing to do
		}

		public void internalWorked(double work) {
			//nothing to do
		}

		public boolean isCanceled() {
			return canceled;
		}

		public void setCanceled(boolean value) {
			canceled = value;
		}

		public void setTaskName(String name) {
			setResult(new Status(IStatus.INFO, InstallerActivator.PI_INSTALLER, name));
		}

		public void subTask(String name) {
			//nothing to do
		}

		public void worked(int work) {
			//nothing to do
		}
	}

	public IStatus performInstall(IInstallOperation operation) {
		return operation.install(new HeadlessProgressMonitor());
	}

	public InstallDescription prepareInstallDescription(InstallDescription description) {
		// The headless advisor has no further input on the install location.
		return description;
	}

	public boolean promptForLaunch(InstallDescription description) {
		return false;
	}

	public void setResult(IStatus status) {
		LogHelper.log(status);
	}

	public void start() {
		//nothing to do
	}

	public void stop() {
		//nothing to do
	}

}
