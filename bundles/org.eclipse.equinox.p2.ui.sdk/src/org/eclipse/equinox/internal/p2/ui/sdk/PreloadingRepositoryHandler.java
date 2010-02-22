/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.swt.widgets.Shell;
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
	 * Execute the command.
	 */
	public Object execute(ExecutionEvent event) {
		doExecuteAndLoad();
		return null;
	}

	void doExecuteAndLoad() {
		if (preloadRepositories()) {
			//cancel any load that is already running
			Job.getJobManager().cancel(LoadMetadataRepositoryJob.LOAD_FAMILY);
			final LoadMetadataRepositoryJob loadJob = new LoadMetadataRepositoryJob(getProvisioningUI());
			setLoadJobProperties(loadJob);
			if (waitForPreload()) {
				loadJob.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (PlatformUI.isWorkbenchRunning())
							if (event.getResult().isOK()) {
								PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
									public void run() {
										doExecute(loadJob);
									}
								});
							}
					}
				});
				loadJob.setUser(true);
				loadJob.schedule();

			} else {
				loadJob.setSystem(true);
				loadJob.setUser(false);
				loadJob.setProperty(LoadMetadataRepositoryJob.WIZARD_CLIENT_SHOULD_SCHEDULE, Boolean.toString(true));
				doExecute(loadJob);
			}
		} else {
			doExecute(null);
		}
	}

	protected abstract void doExecute(LoadMetadataRepositoryJob job);

	protected boolean preloadRepositories() {
		return true;
	}

	protected boolean waitForPreload() {
		return true;
	}

	protected void setLoadJobProperties(Job loadJob) {
		loadJob.setProperty(LoadMetadataRepositoryJob.ACCUMULATE_LOAD_ERRORS, Boolean.toString(true));
	}

	protected ProvisioningUI getProvisioningUI() {
		return ProvisioningUI.getDefaultUI();
	}

	/**
	 * Return a shell appropriate for parenting dialogs of this handler.
	 * @return a Shell
	 */
	protected Shell getShell() {
		return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
	}
}
