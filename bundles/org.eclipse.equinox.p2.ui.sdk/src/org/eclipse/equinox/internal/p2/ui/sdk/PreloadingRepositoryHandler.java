/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.InstallWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

/**
 * PreloadingRepositoryHandler provides background loading of
 * repositories before executing the provisioning handler.
 * 
 * @since 3.5
 */
abstract class PreloadingRepositoryHandler extends AbstractHandler {

	/**
	 * The constructor.
	 */
	public PreloadingRepositoryHandler() {
		// constructor
	}

	/**
	 * Execute the update command.
	 */
	public Object execute(ExecutionEvent event) {
		String profileId;

		// Need to figure out the profile we are using and open a dialog
		try {
			profileId = ProvSDKUIActivator.getSelfProfileId();
		} catch (ProvisionException e) {
			profileId = null;
		}
		if (profileId != null) {
			doExecuteAndLoad(profileId, preloadRepositories());
		} else {
			MessageDialog.openInformation(null, ProvSDKMessages.Handler_SDKUpdateUIMessageTitle, ProvSDKMessages.Handler_CannotLaunchUI);
		}
		return null;
	}

	private void doExecuteAndLoad(final String profileId, boolean preloadRepositories) {
		final QueryableMetadataRepositoryManager queryableManager = new QueryableMetadataRepositoryManager(Policy.getDefault(), false);
		if (preloadRepositories) {
			Job job = new Job(ProvSDKMessages.InstallNewSoftwareHandler_LoadRepositoryJobLabel) {

				protected IStatus run(IProgressMonitor monitor) {
					queryableManager.loadAll(monitor);
					return Status.OK_STATUS;
				}

			};
			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (PlatformUI.isWorkbenchRunning())
						PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
							public void run() {
								doExecute(profileId, queryableManager);
							}
						});
				}
			});
			job.setUser(true);
			job.schedule();
			return;
		}
		doExecute(profileId, queryableManager);
	}

	protected abstract void doExecute(String profileId, QueryableMetadataRepositoryManager manager);

	void openInstallWizard(String profileId, QueryableMetadataRepositoryManager manager) {
		InstallWizard wizard = new InstallWizard(Policy.getDefault(), profileId, null, null, manager);
		WizardDialog dialog = new WizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.INSTALL_WIZARD);

		dialog.open();
	}

	protected boolean preloadRepositories() {
		return true;
	}
}
