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

package org.eclipse.equinox.internal.p2.ui;

import java.io.IOException;
import java.util.HashSet;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.dialogs.ApplyProfileChangesDialog;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.p2.operations.IProfileChangeJob;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants;
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

	HashSet scheduledJobs = new HashSet();
	ListenerList jobListeners = new ListenerList();
	boolean suppressRestart = false;
	ProvisioningUI ui;

	public ProvisioningOperationRunner(ProvisioningUI ui) {
		this.ui = ui;
	}

	/**
	 * Schedule a job to execute the supplied ProvisioningOperation.
	 * 
	 * @param job The operation to execute
	 * @param errorStyle the flags passed to the StatusManager for error reporting
	 */
	public void schedule(final ProvisioningJob job, final int errorStyle) {
		final boolean noPrompt = (errorStyle & (StatusManager.BLOCK | StatusManager.SHOW)) == 0;
		if (noPrompt) {
			job.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
			job.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
		}
		job.setProperty(IProgressConstants.ICON_PROPERTY, ProvUIImages.getImageDescriptor(ProvUIImages.IMG_PROFILE));
		manageJob(job, job.getRestartPolicy());
		job.schedule();
	}

	/**
	 * Request a restart of the platform according to the specified
	 * restart policy.  
	 * 
	 * @param restartPolicy
	 */
	private void requestRestart(final int restartPolicy) {
		// Global override of restart (used in test cases).
		if (suppressRestart)
			return;
		if (restartPolicy == Policy.FORCE_RESTART) {
			PlatformUI.getWorkbench().restart();
			return;
		}
		if (restartPolicy == Policy.FORCE_APPLY) {
			applyProfileChanges();
			return;
		}

		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (PlatformUI.getWorkbench().isClosing())
					return;
				int retCode = ApplyProfileChangesDialog.promptForRestart(ProvUI.getDefaultParentShell(), restartPolicy == Policy.PROMPT_RESTART);
				if (retCode == ApplyProfileChangesDialog.PROFILE_APPLYCHANGES) {
					applyProfileChanges();
				} else if (retCode == ApplyProfileChangesDialog.PROFILE_RESTART) {
					PlatformUI.getWorkbench().restart();
				}
			}
		});
	}

	void applyProfileChanges() {
		Configurator configurator = (Configurator) ServiceHelper.getService(ProvUIActivator.getContext(), Configurator.class.getName());
		try {
			configurator.applyConfiguration();
		} catch (IOException e) {
			ProvUI.handleException(e, ProvUIMessages.ProvUI_ErrorDuringApplyConfig, StatusManager.LOG | StatusManager.BLOCK);
		} catch (IllegalStateException e) {
			IStatus illegalApplyStatus = new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, 0, ProvUIMessages.ProvisioningOperationRunner_CannotApplyChanges, e);
			ProvUI.reportStatus(illegalApplyStatus, StatusManager.LOG | StatusManager.BLOCK);
		}
	}

	public boolean hasScheduledOperationsFor(String profileId) {
		Job[] jobs = getScheduledJobs();
		for (int i = 0; i < jobs.length; i++) {
			if (jobs[i] instanceof IProfileChangeJob) {
				String id = ((IProfileChangeJob) jobs[i]).getProfileId();
				if (profileId.equals(id))
					return true;
			}
		}
		return false;
	}

	public void addJobChangeListener(IJobChangeListener listener) {
		jobListeners.add(listener);
		Job[] jobs = getScheduledJobs();
		for (int i = 0; i < jobs.length; i++)
			jobs[i].addJobChangeListener(listener);
	}

	public void removeJobChangeListener(IJobChangeListener listener) {
		jobListeners.remove(listener);
		Job[] jobs = getScheduledJobs();
		for (int i = 0; i < jobs.length; i++)
			jobs[i].removeJobChangeListener(listener);
	}

	private Job[] getScheduledJobs() {
		return (Job[]) scheduledJobs.toArray(new Job[scheduledJobs.size()]);
	}

	public void manageJob(Job job, final int jobRestartPolicy) {
		scheduledJobs.add(job);
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				scheduledJobs.remove(event.getJob());
				int severity = event.getResult().getSeverity();
				// If the job finished without error, see if restart is needed
				if (severity != IStatus.CANCEL && severity != IStatus.ERROR) {
					if (jobRestartPolicy == ProvisioningJob.RESTART_NONE) {
						return;
					}
					int globalRestartPolicy = ui.getPolicy().getRestartPolicy();
					// If the global policy allows apply changes, check the job policy to see if it supports it.
					if (globalRestartPolicy == Policy.PROMPT_RESTART_OR_APPLY) {
						if (jobRestartPolicy == ProvisioningJob.RESTART_OR_APPLY)
							requestRestart(Policy.PROMPT_RESTART_OR_APPLY);
						else
							requestRestart(Policy.PROMPT_RESTART);
					} else
						requestRestart(globalRestartPolicy);
				}
			}
		});
		Object[] listeners = jobListeners.getListeners();
		for (int i = 0; i < listeners.length; i++)
			job.addJobChangeListener((IJobChangeListener) listeners[i]);
	}

	/**
	 * This method is provided for use in automated test case.  It should
	 * no longer be needed to be used by clients.
	 * 
	 * @param suppress <code>true</code> to suppress all restarts and <code>false</code>
	 * to stop suppressing restarts.
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void suppressRestart(boolean suppress) {
		suppressRestart = suppress;
	}
}
