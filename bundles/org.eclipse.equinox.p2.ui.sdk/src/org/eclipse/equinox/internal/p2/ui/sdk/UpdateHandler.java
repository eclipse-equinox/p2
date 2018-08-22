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
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * UpdateHandler invokes the check for updates UI
 * 
 * @since 3.4
 */
public class UpdateHandler extends PreloadingRepositoryHandler {

	boolean hasNoRepos = false;
	UpdateOperation operation;

	@Override
	protected void doExecute(LoadMetadataRepositoryJob job) {
		if (hasNoRepos) {
			if (getProvisioningUI().getPolicy().getRepositoriesVisible()) {
				boolean goToSites = MessageDialog.openQuestion(getShell(), ProvSDKMessages.UpdateHandler_NoSitesTitle, ProvSDKMessages.UpdateHandler_NoSitesMessage);
				if (goToSites) {
					getProvisioningUI().manipulateRepositories(getShell());
				}
			}
			return;
		}
		// Report any missing repositories.
		job.reportAccumulatedStatus();
		if (getProvisioningUI().getPolicy().continueWorkingWithOperation(operation, getShell())) {

			if (operation.getResolutionResult() == Status.OK_STATUS) {
				getProvisioningUI().openUpdateWizard(false, operation, job);
			} else {

				final RemediationOperation remediationOperation = new RemediationOperation(getProvisioningUI().getSession(), operation.getProfileChangeRequest(), RemedyConfig.getCheckForUpdateRemedyConfigs());
				ProvisioningJob job2 = new ProvisioningJob(ProvSDKMessages.RemediationOperation_ResolveJobName, getProvisioningUI().getSession()) {
					@Override
					public IStatus runModal(IProgressMonitor monitor) {
						monitor.beginTask(ProvSDKMessages.RemediationOperation_ResolveJobTask, RemedyConfig.getAllRemedyConfigs().length);
						return remediationOperation.resolveModal(monitor);
					}
				};
				job2.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						if (PlatformUI.isWorkbenchRunning()) {
							PlatformUI.getWorkbench().getDisplay().asyncExec(() -> getProvisioningUI().openUpdateWizard(true, operation, remediationOperation, null));
						}
					}

				});
				getProvisioningUI().schedule(job2, StatusManager.SHOW | StatusManager.LOG);
			}
		}
	}

	@Override
	protected void doPostLoadBackgroundWork(IProgressMonitor monitor) throws OperationCanceledException {
		operation = getProvisioningUI().getUpdateOperation(null, null);
		// check for updates
		IStatus resolveStatus = operation.resolveModal(monitor);
		if (resolveStatus.getSeverity() == IStatus.CANCEL)
			throw new OperationCanceledException();
	}

	@Override
	protected boolean preloadRepositories() {
		hasNoRepos = false;
		RepositoryTracker repoMan = getProvisioningUI().getRepositoryTracker();
		if (repoMan.getKnownRepositories(getProvisioningUI().getSession()).length == 0) {
			hasNoRepos = true;
			return false;
		}
		return super.preloadRepositories();
	}

	@Override
	protected String getProgressTaskName() {
		return ProvSDKMessages.UpdateHandler_ProgressTaskName;
	}
}
