/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.io.IOException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.dialogs.ApplyProfileChangesDialog;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.ui.progress.IProgressConstants2;
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

	boolean suppressRestart = false;
	ProvisioningUI ui;
	public int eventBatchCount = 0;

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
		job.setProperty(IProgressConstants2.SHOW_IN_TASKBAR_ICON_PROPERTY, Boolean.TRUE);
		manageJob(job, job.getRestartPolicy());
		job.schedule();
	}

	/**
	 * Request a restart of the platform according to the specified
	 * restart policy.  
	 * 
	 * @param restartPolicy
	 */
	void requestRestart(final int restartPolicy) {
		// Global override of restart (used in test cases).
		if (suppressRestart)
			return;
		if (restartPolicy == Policy.RESTART_POLICY_FORCE) {
			PlatformUI.getWorkbench().restart();
			return;
		}
		if (restartPolicy == Policy.RESTART_POLICY_FORCE_APPLY) {
			applyProfileChanges();
			return;
		}

		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (PlatformUI.getWorkbench().isClosing())
					return;
				int retCode = ApplyProfileChangesDialog.promptForRestart(ProvUI.getDefaultParentShell(), restartPolicy == Policy.RESTART_POLICY_PROMPT);
				if (retCode == ApplyProfileChangesDialog.PROFILE_APPLYCHANGES) {
					applyProfileChanges();
				} else if (retCode == ApplyProfileChangesDialog.PROFILE_RESTART) {
					PlatformUI.getWorkbench().restart(true);
				}
			}
		});
	}

	void applyProfileChanges() {
		Configurator configurator = ServiceHelper.getService(ProvUIActivator.getContext(), Configurator.class);
		try {
			configurator.applyConfiguration();
		} catch (IOException e) {
			ProvUI.handleException(e, ProvUIMessages.ProvUI_ErrorDuringApplyConfig, StatusManager.LOG | StatusManager.BLOCK);
		} catch (IllegalStateException e) {
			IStatus illegalApplyStatus = new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, 0, ProvUIMessages.ProvisioningOperationRunner_CannotApplyChanges, e);
			ProvUI.reportStatus(illegalApplyStatus, StatusManager.LOG | StatusManager.BLOCK);
		}
	}

	public void manageJob(Job job, final int jobRestartPolicy) {
		ui.getSession().rememberJob(job);
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				int severity = event.getResult().getSeverity();
				// If the job finished without error, see if restart is needed
				if (severity != IStatus.CANCEL && severity != IStatus.ERROR) {
					if (jobRestartPolicy == ProvisioningJob.RESTART_NONE) {
						return;
					}
					int globalRestartPolicy = ui.getPolicy().getRestartPolicy();
					// If the global policy allows apply changes, check the job policy to see if it supports it.
					if (globalRestartPolicy == Policy.RESTART_POLICY_PROMPT_RESTART_OR_APPLY) {
						if (jobRestartPolicy == ProvisioningJob.RESTART_OR_APPLY)
							requestRestart(Policy.RESTART_POLICY_PROMPT_RESTART_OR_APPLY);
						else
							requestRestart(Policy.RESTART_POLICY_PROMPT);
					} else
						requestRestart(globalRestartPolicy);
				}
			}
		});
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
