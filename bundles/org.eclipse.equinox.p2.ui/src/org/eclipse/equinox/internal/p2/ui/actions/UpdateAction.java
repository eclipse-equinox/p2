/*******************************************************************************
 *  Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.actions;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

public class UpdateAction extends ExistingIUInProfileAction {

	protected IUElementListRoot root; // root that will be used to seed the wizard
	protected ArrayList<?> initialSelections; // the elements that should be selected in the wizard
	boolean resolveIsVisible = true;
	boolean skipSelectionPage = false;

	public UpdateAction(ProvisioningUI ui, ISelectionProvider selectionProvider, String profileId, boolean resolveIsVisible) {
		super(ui, ProvUI.UPDATE_COMMAND_LABEL, selectionProvider, profileId);
		setToolTipText(ProvUI.UPDATE_COMMAND_TOOLTIP);
		this.resolveIsVisible = resolveIsVisible;
	}

	public void setSkipSelectionPage(boolean skipSelectionPage) {
		this.skipSelectionPage = skipSelectionPage;
	}

	protected String getTaskName() {
		return ProvUIMessages.UpdateIUProgress;
	}

	protected boolean isResolveUserVisible() {
		return resolveIsVisible;
	}

	@Override
	protected int getLockConstant() {
		return IProfile.LOCK_UPDATE;
	}

	@Override
	protected ProfileChangeOperation getProfileChangeOperation(Collection<IInstallableUnit> ius) {
		return ui.getUpdateOperation(ius, null);
	}

	@Override
	protected int performAction(final ProfileChangeOperation operation, Collection<IInstallableUnit> ius) {
		if (operation.getResolutionResult() == Status.OK_STATUS)
			return ui.openUpdateWizard(skipSelectionPage, (UpdateOperation) operation, null);

		if (!operation.hasResolved())
			return Window.CANCEL;

		final RemediationOperation remediationOperation = new RemediationOperation(getSession(), operation.getProfileChangeRequest());
		ProvisioningJob job = new ProvisioningJob(ProvUIMessages.UpdateActionRemediationJobName, getSession()) {
			@Override
			public IStatus runModal(IProgressMonitor monitor) {
				monitor.beginTask(ProvUIMessages.UpdateActionRemediationJobTask, RemedyConfig.getAllRemedyConfigs().length);
				return remediationOperation.resolveModal(monitor);
			}
		};
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (PlatformUI.isWorkbenchRunning()) {
					PlatformUI.getWorkbench().getDisplay().asyncExec(() -> ui.openUpdateWizard(skipSelectionPage, (UpdateOperation) operation, remediationOperation, null));
				}
			}

		});
		getProvisioningUI().schedule(job, StatusManager.SHOW | StatusManager.LOG);
		return Window.CANCEL;
	}
}
