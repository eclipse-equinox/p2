/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui;

import java.io.IOException;
import java.util.HashSet;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.ApplyProfileChangesDialog;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation;
import org.eclipse.osgi.util.NLS;
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
	static boolean subsequentRestartsRequested = false;
	// used during automated testing to prevent a restart dialog from interrupting tests
	static boolean suppressRestart = false;
	static ListenerList jobListeners = new ListenerList();

	/**
	 * This method is temporary and will not appear in the final API.
	 * 
	 * @param suppress
	 * 
	 * @deprecated see https://bugs.eclipse.org/bugs/show_bug.cgi?id=274876
	 */
	public static void suppressRestart(boolean suppress) {
		suppressRestart = suppress;
	}

	/**
	 * Run the provisioning operation synchronously
	 * @param op The operation to execute
	 * @param errorStyle the flags passed to the StatusManager for error reporting
	 */
	public static void run(ProvisioningOperation op, int errorStyle) {
		try {
			op.execute(new NullProgressMonitor());
		} catch (OperationCanceledException e) {
			// nothing to do
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null, errorStyle);
		}

	}

	/**
	 * Schedule a job to execute the supplied ProvisioningOperation.
	 * 
	 * @param op The operation to execute
	 * @param errorStyle the flags passed to the StatusManager for error reporting
	 */
	public static Job schedule(final ProvisioningOperation op, final int errorStyle) {
		Job job;
		final boolean noPrompt = (errorStyle & (StatusManager.BLOCK | StatusManager.SHOW)) == 0;

		if (op.runInBackground()) {
			job = new Job(op.getLabel()) {
				protected IStatus run(IProgressMonitor monitor) {
					final Job thisJob = this;
					try {
						IStatus status = op.execute(monitor);
						if (!status.isOK() && noPrompt) {
							this.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
							this.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
						}
						return status;
					} catch (OperationCanceledException e) {
						return Status.CANCEL_STATUS;
					} catch (final ProvisionException e) {
						if (noPrompt) {
							thisJob.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
							thisJob.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
						}
						String message = e.getLocalizedMessage();
						if (message == null)
							message = NLS.bind(ProvUIMessages.ProvisioningOperationRunner_ErrorExecutingOperation, op.getLabel());
						return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, message, e);
					}
				}
			};
			job.setPriority(Job.LONG);
		} else {
			job = new WorkbenchJob(op.getLabel()) {
				public IStatus runInUIThread(IProgressMonitor monitor) {
					try {
						IStatus status = op.execute(monitor);
						if (!status.isOK() && noPrompt) {
							this.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
							this.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
						}
						return status;
					} catch (OperationCanceledException e) {
						return Status.CANCEL_STATUS;
					} catch (ProvisionException e) {
						if (noPrompt) {
							this.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
							this.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
						}
						return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, NLS.bind(ProvUIMessages.ProvisioningOperationRunner_ErrorExecutingOperation, op.getLabel()), e);
					}
				}
			};
			job.setPriority(Job.SHORT);
		}
		job.setUser(op.isUser());
		job.setProperty(OPERATION_KEY, op);
		job.setProperty(IProgressConstants.ICON_PROPERTY, ProvUIImages.getImageDescriptor(ProvUIImages.IMG_PROFILE));
		manageJob(job);
		job.schedule();
		return job;
	}

	/**
	 * This method is temporary and is not intended for the API.
	 * 
	 * @deprecated see https://bugs.eclipse.org/bugs/show_bug.cgi?id=274876
	 */
	public static void clearRestartRequests() {
		restartRequired = false;
		restartRequested = false;
		subsequentRestartsRequested = false;
	}

	/**
	 * This method will not appear in the final API.
	 * 
	 * @param force
	 * @deprecated see https://bugs.eclipse.org/bugs/show_bug.cgi?id=274876

	 */
	public static void requestRestart(boolean force) {
		if (suppressRestart || hasScheduledOperations()) {
			restartRequested = true;
			subsequentRestartsRequested = true;
			restartRequired = restartRequired || force;
			return;
		}
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (PlatformUI.getWorkbench().isClosing())
					return;
				int retCode = ApplyProfileChangesDialog.promptForRestart(ProvUI.getDefaultParentShell(), restartRequired);
				// Now that we have asked, regardless of answer, we won't need to
				// ask again until the next profile changing operation.  Don't reset
				// the restart required flag so that the next time we ask, if it
				// was required before, it will still be required.
				restartRequested = false;
				if (retCode == ApplyProfileChangesDialog.PROFILE_APPLYCHANGES) {
					Configurator configurator = (Configurator) ServiceHelper.getService(ProvUIActivator.getContext(), Configurator.class.getName());
					try {
						configurator.applyConfiguration();
					} catch (IOException e) {
						ProvUI.handleException(e, ProvUIMessages.ProvUI_ErrorDuringApplyConfig, StatusManager.LOG | StatusManager.BLOCK);
					} catch (IllegalStateException e) {
						IStatus illegalApplyStatus = new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, 0, ProvUIMessages.ProvisioningOperationRunner_CannotApplyChanges, e);
						ProvUI.reportStatus(illegalApplyStatus, StatusManager.LOG | StatusManager.BLOCK);
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

	public static void manageJob(Job job) {
		scheduledJobs.add(job);
		subsequentRestartsRequested = false;
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				scheduledJobs.remove(event.getJob());
				int severity = event.getResult().getSeverity();
				if (severity != IStatus.CANCEL && severity != IStatus.ERROR && restartRequested) {
					requestRestart(restartRequired);
				} else {
					restartRequested = subsequentRestartsRequested;
				}
			}
		});
		Object[] listeners = jobListeners.getListeners();
		for (int i = 0; i < listeners.length; i++)
			job.addJobChangeListener((IJobChangeListener) listeners[i]);
	}
}
