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
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants;
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

	private static final String PROPERTY_PREFIX = "org.eclipse.equinox.p2.ui"; //$NON-NLS-1$
	private static final QualifiedName OPERATION_KEY = new QualifiedName(PROPERTY_PREFIX, "operationKey"); //$NON-NLS-1$
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
	 * @param errorStyle the flags passed to the StatusManager for error reporting
	 */
	public static void run(ProvisioningOperation op, Shell shell, int errorStyle) {
		try {
			if (op instanceof IUndoableOperation) {
				PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute((IUndoableOperation) op, null, ProvUI.getUIInfoAdapter(shell));
			} else {
				op.execute(null, ProvUI.getUIInfoAdapter(shell));
			}
		} catch (ExecutionException e) {
			ProvUI.handleException(e.getCause(), NLS.bind(ProvUIMessages.ProvisioningOperationRunner_ErrorExecutingOperation, op.getLabel()), errorStyle);
		}

	}

	/**
	 * Schedule a job to execute the supplied ProvisioningOperation, and add it to the
	 * undo history if it supports undo.
	 * 
	 * @param op The operation to execute
	 * @param shell provided by the caller in order to supply UI information for prompting the
	 *            user if necessary. May be <code>null</code>.
	 * @param errorStyle the flags passed to the StatusManager for error reporting
	 */
	public static Job schedule(final ProvisioningOperation op, final Shell shell, final int errorStyle) {
		Job job;
		final boolean noPrompt = (errorStyle & (StatusManager.BLOCK | StatusManager.SHOW)) == 0;

		if (op.runInBackground()) {
			job = new Job(op.getLabel()) {
				protected IStatus run(IProgressMonitor monitor) {
					final Job thisJob = this;
					try {
						IStatus status;
						if (op instanceof IUndoableOperation) {
							status = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute((IUndoableOperation) op, monitor, ProvUI.getUIInfoAdapter(shell));
						} else {
							status = op.execute(monitor, ProvUI.getUIInfoAdapter(shell));
						}
						if (status != Status.OK_STATUS && noPrompt) {
							this.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
							this.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
						}
						return status;
					} catch (final ExecutionException e) {
						if (noPrompt) {
							thisJob.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
							thisJob.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
						}
						String message = e.getCause().getLocalizedMessage();
						if (message == null)
							message = NLS.bind(ProvUIMessages.ProvisioningOperationRunner_ErrorExecutingOperation, op.getLabel());
						return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, message, e.getCause());
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
						if (status != Status.OK_STATUS && noPrompt) {
							this.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
							this.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
						}
						return status;
					} catch (ExecutionException e) {
						if (noPrompt) {
							this.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
							this.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
						}
						return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, NLS.bind(ProvUIMessages.ProvisioningOperationRunner_ErrorExecutingOperation, op.getLabel()), e.getCause());
					}
				}
			};
			job.setPriority(Job.SHORT);
		}
		job.setUser(op.isUser());
		job.setProperty(OPERATION_KEY, op);
		job.setProperty(IProgressConstants.ICON_PROPERTY, ProvUIImages.getImageDescriptor(ProvUIImages.IMG_PROFILE));
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
		if (hasScheduledOperations()) {
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

	public static boolean hasScheduledOperations() {
		return !scheduledJobs.isEmpty();
	}

	public static boolean hasScheduledOperationsFor(String profileId) {
		Job[] jobs = getScheduledJobs();
		for (int i = 0; i < jobs.length; i++) {
			Object op = jobs[i].getProperty(OPERATION_KEY);
			if (op instanceof ProfileModificationOperation) {
				String id = ((ProfileModificationOperation) op).getProfileId();
				if (profileId.equals(id))
					return true;
			}
		}
		return false;
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

	private static Job[] getScheduledJobs() {
		return (Job[]) scheduledJobs.toArray(new Job[scheduledJobs.size()]);
	}

}
