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

package org.eclipse.equinox.p2.ui;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.ui.operations.ProvisioningOperation;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Utility methods for running provisioning operations
 * 
 * @since 3.4
 */
public class ProvisioningOperationRunner {

	private static class ProvisioningJob extends Job {
		private ProvisioningOperation op;
		private ProvisioningOperationResult result;

		ProvisioningJob(ProvisioningOperation op, ProvisioningOperationResult result) {
			super(op.getLabel());
			this.op = op;
			this.result = result;
		}

		protected IStatus run(IProgressMonitor monitor) {
			try {
				IStatus status;
				if (op instanceof IUndoableOperation) {
					status = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute((IUndoableOperation) op, monitor, null);
				} else {
					status = op.execute(monitor, null);
				}
				result.setStatus(status);
				return status;
			} catch (ExecutionException e) {
				return ProvUI.handleException(e.getCause(), null);
			}
		}

		public boolean belongsTo(Object family) {
			return family == FAMILY_PROVISIONING_OPERATIONS;
		}

	}

	private static Object FAMILY_PROVISIONING_OPERATIONS = new Object();

	/**
	 * Execute the supplied ProvisioningOperation, and add it to the
	 * undo history if it supports undo.
	 * 
	 * @param op The operation to execute
	 * @param shell provided by the caller in order to supply UI information for prompting the
	 *            user if necessary. May be <code>null</code>.
	 * @param context the IRunnableContext provided by the caller for running the operation.  May be <code>null</code>, in which case
	 *            a runnable context will be created based on whether the operation should be run in the foreground or background.
	 *            Callers typically need not supply a context unless special handling (such as wizard-based progress reporting) is required.
	*/
	// TODO get rid of context parameter and result after M3
	// should just return the job and callers can hook listeners if they care what's happening
	public static ProvisioningOperationResult execute(final ProvisioningOperation op, final Shell shell, IRunnableContext context) {
		final ProvisioningOperationResult result = new ProvisioningOperationResult(op);
		Job job;

		if (op.runInBackground()) {
			job = new ProvisioningJob(op, result);
			job.setPriority(Job.DECORATE); // this is the prio that the old update manager used
		} else {
			job = new WorkbenchJob(op.getLabel()) {

				public IStatus runInUIThread(IProgressMonitor monitor) {
					try {
						IStatus status;
						if (op instanceof IUndoableOperation) {
							status = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute((IUndoableOperation) op, monitor, ProvUI.getUIInfoAdapter(shell));
						} else {
							status = op.execute(monitor, ProvUI.getUIInfoAdapter(shell));
						}
						result.setStatus(status);
						return status;
					} catch (ExecutionException e) {
						return ProvUI.handleException(e.getCause(), null);
					}
				}

				public boolean belongsTo(Object family) {
					return family == FAMILY_PROVISIONING_OPERATIONS;
				}

			};
		}
		job.setUser(op.isUser());
		job.schedule();
		result.setJob(job);
		return result;
	}
}
