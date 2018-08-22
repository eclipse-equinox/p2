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

		@Override
		public void beginTask(String name, int totalWork) {
			setResult(new Status(IStatus.INFO, InstallerActivator.PI_INSTALLER, name));
		}

		@Override
		public void done() {
			//nothing to do
		}

		@Override
		public void internalWorked(double work) {
			//nothing to do
		}

		@Override
		public boolean isCanceled() {
			return canceled;
		}

		@Override
		public void setCanceled(boolean value) {
			canceled = value;
		}

		@Override
		public void setTaskName(String name) {
			setResult(new Status(IStatus.INFO, InstallerActivator.PI_INSTALLER, name));
		}

		@Override
		public void subTask(String name) {
			//nothing to do
		}

		@Override
		public void worked(int work) {
			//nothing to do
		}
	}

	@Override
	public IStatus performInstall(IInstallOperation operation) {
		return operation.install(new HeadlessProgressMonitor());
	}

	@Override
	public InstallDescription prepareInstallDescription(InstallDescription description) {
		// The headless advisor has no further input on the install location.
		return description;
	}

	@Override
	public boolean promptForLaunch(InstallDescription description) {
		return false;
	}

	@Override
	public void setResult(IStatus status) {
		LogHelper.log(status);
	}

	@Override
	public void start() {
		//nothing to do
	}

	@Override
	public void stop() {
		//nothing to do
	}

}
