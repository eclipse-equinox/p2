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

package org.eclipse.equinox.internal.provisional.p2.ui;

import java.io.IOException;
import java.util.HashSet;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.ApplyProfileChangesDialog;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Utility methods for running provisioning operations.   Operations can either
 * be run synchronously or in a job.  When scheduled as a job, the operation
 * determines whether the job is run in
 * the background or in the UI.
 * 
 * @since 3.4
 */
public class ProvisioningOperationRunner {

	static HashSet scheduledJobs = new HashSet();
	static boolean restartRequested = false;
	static boolean restartRequired = false;
	static ListenerList jobListeners = new ListenerList();

	/**
	 * Run the provisioning operation synchronously, adding it to the undo history if it
	 * supports undo.  Should only be used for operations that run quickly.
	 * @param op The operation to execute
	 * @param shell provided by the caller in order to supply UI information for prompting the
	 *            user if necessary. May be <code>null</code>.
	 */
	public static void run(ProvisioningOperation op, Shell shell) {
		try {
			if (op instanceof IUndoableOperation) {
				PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute((IUndoableOperation) op, null, ProvUI.getUIInfoAdapter(shell));
			} else {
				op.execute(null, ProvUI.getUIInfoAdapter(shell));
			}
		} catch (ExecutionException e) {
			ProvUI.handleException(e.getCause(), NLS.bind(ProvUIMessages.ProvisioningOperationRunner_ErrorExecutingOperation, op.getLabel()), StatusManager.SHOW | StatusManager.LOG);
		}

	}

	/**
	 * Schedule a job to execute the supplied ProvisioningOperation, and add it to the
	 * undo history if it supports undo.
	 * 
	 * @param op The operation to execute
	 * @param shell provided by the caller in order to supply UI information for prompting the
	 *            user if necessary. May be <code>null</code>.
	 */
	public static Job schedule(final ProvisioningOperation op, final Shell shell) {
		Job job;

		if (op.runInBackground()) {
			job = new Job(op.getLabel()) {
				protected IStatus run(IProgressMonitor monitor) {
					try {
						IStatus status;
						if (op instanceof IUndoableOperation) {
							status = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute((IUndoableOperation) op, monitor, ProvUI.getUIInfoAdapter(shell));
						} else {
							status = op.execute(monitor, ProvUI.getUIInfoAdapter(shell));
						}
						return status;
					} catch (final ExecutionException e) {
						final IStatus[] status = new IStatus[1];
						shell.getDisplay().asyncExec(new Runnable() {
							public void run() {
								status[0] = ProvUI.handleException(e.getCause(), NLS.bind(ProvUIMessages.ProvisioningOperationRunner_ErrorExecutingOperation, op.getLabel()), StatusManager.SHOW | StatusManager.LOG);

							}
						});
						return status[0];
					}
				}
			};
			job.setPriority(Job.LONG);
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
						return status;
					} catch (ExecutionException e) {
						return ProvUI.handleException(e.getCause(), NLS.bind(ProvUIMessages.ProvisioningOperationRunner_ErrorExecutingOperation, op.getLabel()), StatusManager.SHOW | StatusManager.LOG);
					}
				}
			};
			job.setPriority(Job.SHORT);
		}
		job.setUser(op.isUser());
		scheduledJobs.add(job);
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				scheduledJobs.remove(event.getJob());
				if (restartRequested) {
					requestRestart(restartRequired);
				}
			}
		});
		Object[] listeners = jobListeners.getListeners();
		for (int i = 0; i < listeners.length; i++)
			job.addJobChangeListener((IJobChangeListener) listeners[i]);
		job.schedule();
		return job;
	}

	public static void requestRestart(boolean force) {
		if (isRunningOperations()) {
			restartRequested = true;
			restartRequired = restartRequired || force;
			return;
		}
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (PlatformUI.getWorkbench().isClosing())
					return;
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				if (window == null)
					return;
				int retCode = ApplyProfileChangesDialog.promptForRestart(window.getShell(), restartRequired);
				if (retCode == ApplyProfileChangesDialog.PROFILE_APPLYCHANGES) {
					Configurator configurator = (Configurator) ServiceHelper.getService(ProvUIActivator.getContext(), Configurator.class.getName());
					try {
						configurator.applyConfiguration();
					} catch (IOException e) {
						ProvUI.handleException(e, ProvUIMessages.ProvUI_ErrorDuringApplyConfig, StatusManager.LOG | StatusManager.BLOCK);
					}
				} else if (retCode == ApplyProfileChangesDialog.PROFILE_RESTART) {
					PlatformUI.getWorkbench().restart();
				}
			}
		});
	}

	private static boolean isRunningOperations() {
		return !scheduledJobs.isEmpty();
	}

	public static void addJobChangeListener(IJobChangeListener listener) {
		jobListeners.add(listener);
		Job[] jobs = getScheduledJobs();
		for (int i = 0; i < jobs.length; i++)
			jobs[i].addJobChangeListener(listener);
	}

	public static void removeJobChangeListener(IJobChangeListener listener) {
		jobListeners.remove(listener);
		Job[] jobs = getScheduledJobs();
		for (int i = 0; i < jobs.length; i++)
			jobs[i].removeJobChangeListener(listener);
	}

	public static Job[] getScheduledJobs() {
		return (Job[]) scheduledJobs.toArray(new Job[scheduledJobs.size()]);
	}
}
