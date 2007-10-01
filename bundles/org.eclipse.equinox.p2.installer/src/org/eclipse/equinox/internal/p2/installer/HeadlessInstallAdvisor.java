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
package org.eclipse.equinox.internal.p2.installer;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.installer.InstallAdvisor;
import org.eclipse.equinox.p2.installer.IInstallDescription;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * A headless install advisor that prints everything to a log.
 */
public class HeadlessInstallAdvisor extends InstallAdvisor {
	class HeadlessProgressMonitor implements IProgressMonitor {
		private boolean canceled;

		public void beginTask(String name, int totalWork) {
			reportStatus(new Status(IStatus.INFO, InstallerActivator.PI_INSTALLER, name));
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
			reportStatus(new Status(IStatus.INFO, InstallerActivator.PI_INSTALLER, name));
		}

		public void subTask(String name) {
			//nothing to do
		}

		public void worked(int work) {
			//nothing to do
		}
	}

	class HeadlessRunnableContext implements IRunnableContext {
		public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
			runnable.run(new HeadlessProgressMonitor());
		}
	}

	public String getInstallLocation(IInstallDescription description) {
		// The headless advisor has no further input on the install location.
		return null;
	}

	public IRunnableContext getRunnableContext() {
		return new HeadlessRunnableContext();
	}

	public void reportStatus(IStatus status) {
		LogHelper.log(status);
	}

	public void start() {
		//nothing to do
	}

	public void stop() {
		//nothing to do
	}

}
