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

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.IProvHelpContextIds;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.ProvisioningWizardDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.UninstallWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

public class UninstallAction extends ExistingIUInProfileAction {

	public UninstallAction(Policy policy, ISelectionProvider selectionProvider, String profileId) {
		super(ProvUI.UNINSTALL_COMMAND_LABEL, policy, selectionProvider, profileId);
		setToolTipText(ProvUI.UNINSTALL_COMMAND_TOOLTIP);
	}

	protected String getTaskName() {
		return ProvUIMessages.UninstallIUProgress;
	}

	protected int performAction(IInstallableUnit[] ius, String targetProfileId, PlannerResolutionOperation resolution) {
		UninstallWizard wizard = new UninstallWizard(getPolicy(), targetProfileId, ius, resolution);
		WizardDialog dialog = new ProvisioningWizardDialog(getShell(), wizard);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.UNINSTALL_WIZARD);

		return dialog.open();
	}

	protected ProfileChangeRequest getProfileChangeRequest(IInstallableUnit[] ius, String targetProfileId, MultiStatus status, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, ProvUIMessages.ProfileChangeRequestBuildingRequest, 1);
		ProfileChangeRequest request = null;
		try {
			request = ProfileChangeRequest.createByProfileId(targetProfileId);
			request.removeInstallableUnits(ius);
			// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=255984
			// We ask to remove the the profile root property in addition to removing the IU.  In theory this
			// should be redundant, but there are cases where the planner decides not to uninstall something because
			// it is needed by others.  We still want to remove the root in this case.
			String key = getPolicy().getQueryContext().getVisibleInstalledIUProperty();
			for (int i = 0; i < ius.length; i++)
				request.removeInstallableUnitProfileProperty(ius[i], key);
		} finally {
			sub.done();
		}
		return request;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.actions.AlterExistingProfileIUAction#getLockConstant()
	 */
	protected int getLockConstant() {
		return IInstallableUnit.LOCK_UNINSTALL;
	}
}
