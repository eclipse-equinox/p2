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

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.ui.operations.ProvisioningOperation;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Utility methods for running provisioning operations
 * 
 * @since 3.4
 */
public class ProvisioningOperationRunner {

	private static Object FAMILY_PROVISIONING_OPERATIONS = new Object();

	private static class JobRunnableContext implements IRunnableContext {

		private class ProvisioningJob extends Job {
			private IRunnableWithProgress runnable;

			ProvisioningJob(String name, IRunnableWithProgress runnable) {
				super(name);
				this.runnable = runnable;
			}

			protected IStatus run(IProgressMonitor monitor) {
				try {
					runnable.run(monitor);
				} catch (InvocationTargetException e) {
					return ProvUI.handleException(e, null);
				} catch (InterruptedException e) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}

			public boolean belongsTo(Object family) {
				return family == FAMILY_PROVISIONING_OPERATIONS;
			}

		}

		private ProvisioningOperation op;
		private ProvisioningOperationResult result;

		JobRunnableContext(ProvisioningOperation operation, ProvisioningOperationResult result) {
			this.op = operation;
			this.result = result;
		}

		public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) {
			Job job = new ProvisioningJob(op.getLabel(), runnable);
			job.setUser(op.isUser());
			job.setPriority(Job.DECORATE); // this is the prio that the old update manager used
			job.schedule();
			result.setJob(job);
		}

	}

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
	public static ProvisioningOperationResult execute(final ProvisioningOperation op, final Shell shell, IRunnableContext context) {

		final ProvisioningOperationResult[] result = new ProvisioningOperationResult[1];
		result[0] = new ProvisioningOperationResult(op);
		result[0].setStatus(Status.OK_STATUS);
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
					if (op instanceof IUndoableOperation) {
						result[0].setStatus(PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute((IUndoableOperation) op, monitor, ProvUI.getUIInfoAdapter(shell)));
					} else {
						result[0].setStatus(op.execute(monitor, ProvUI.getUIInfoAdapter(shell)));
					}
				} catch (ExecutionException e) {
					result[0].setStatus(ProvUI.handleException(e.getCause(), null));
				}
			}
		};
		if (context == null) {
			if (op.runInBackground())
				context = new JobRunnableContext(op, result[0]);
			else
				context = PlatformUI.getWorkbench().getProgressService();
		}
		try {
			context.run(op.runInBackground(), true, runnable);
		} catch (InterruptedException e) {
			// do nothing
		} catch (InvocationTargetException e) {
			result[0].setStatus(ProvUI.handleException(e.getCause(), null));
		}
		return result[0];
	}
}
