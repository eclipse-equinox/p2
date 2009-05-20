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

package org.eclipse.equinox.internal.provisional.p2.ui.actions;

import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.PlanAnalyzer;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.ProvisioningWizardDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.UpdateWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

public class UpdateAction extends ExistingIUInProfileAction {

	protected IUElementListRoot root; // root that will be used to seed the wizard
	protected ArrayList initialSelections; // the elements that should be selected in the wizard
	boolean resolveIsVisible = true;
	QueryableMetadataRepositoryManager manager;
	boolean skipSelectionPage = false;

	public UpdateAction(Policy policy, ISelectionProvider selectionProvider, String profileId, boolean resolveIsVisible) {
		super(ProvUI.UPDATE_COMMAND_LABEL, policy, selectionProvider, profileId);
		setToolTipText(ProvUI.UPDATE_COMMAND_TOOLTIP);
		this.resolveIsVisible = resolveIsVisible;
	}

	public void setRepositoryManager(QueryableMetadataRepositoryManager manager) {
		this.manager = manager;
	}

	public void setSkipSelectionPage(boolean skipSelectionPage) {
		this.skipSelectionPage = skipSelectionPage;
	}

	protected int performAction(IInstallableUnit[] ius, String targetProfileId, PlannerResolutionOperation resolution) {
		// Caches should have been created while formulating the plan
		Assert.isNotNull(initialSelections);
		Assert.isNotNull(root);
		Assert.isNotNull(resolution);

		UpdateWizard wizard = new UpdateWizard(getPolicy(), targetProfileId, root, initialSelections.toArray(), resolution, manager);
		wizard.setSkipSelectionsPage(skipSelectionPage);
		WizardDialog dialog = new ProvisioningWizardDialog(getShell(), wizard);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.UPDATE_WIZARD);

		return dialog.open();
	}

	protected ProfileChangeRequest getProfileChangeRequest(IInstallableUnit[] ius, String targetProfileId, MultiStatus status, IProgressMonitor monitor) {
		initialSelections = new ArrayList();
		root = new IUElementListRoot();
		ProfileChangeRequest request = UpdateWizard.createProfileChangeRequest(ius, targetProfileId, root, initialSelections, monitor);
		if (request == null) {
			status.add(PlanAnalyzer.getStatus(IStatusCodes.NOTHING_TO_UPDATE, null));
			return null;
		}

		return request;
	}

	protected String getTaskName() {
		return ProvUIMessages.UpdateIUProgress;
	}

	protected boolean isResolveUserVisible() {
		return resolveIsVisible;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.actions.AlterExistingProfileIUAction#getLockConstant()
	 */
	protected int getLockConstant() {
		return IInstallableUnit.LOCK_UPDATE;
	}

	protected IStatus getNoProfileOrSelectionStatus(String id, IInstallableUnit[] ius) {
		if (ius.length == 0)
			return PlanAnalyzer.getStatus(IStatusCodes.NOTHING_TO_UPDATE, null);
		return super.getNoProfileOrSelectionStatus(id, ius);
	}
}
