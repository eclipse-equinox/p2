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

package org.eclipse.equinox.internal.provisional.p2.ui.actions;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.IUElement;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.IProvHelpContextIds;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.UninstallWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

public class UninstallAction extends ProfileModificationAction {

	public UninstallAction(Policy policy, ISelectionProvider selectionProvider, String profileId) {
		super(policy, ProvUI.UNINSTALL_COMMAND_LABEL, selectionProvider, profileId);
		setToolTipText(ProvUI.UNINSTALL_COMMAND_TOOLTIP);
	}

	protected boolean isEnabledFor(Object[] selectionArray) {
		Object parent = null;
		// We don't want to prompt for a profile during validation,
		// so we only consider the profile id.
		IProfile profile = getProfile(false);
		if (profile == null)
			return false;
		if (selectionArray.length > 0) {
			for (int i = 0; i < selectionArray.length; i++) {
				if (selectionArray[i] instanceof InstalledIUElement) {
					InstalledIUElement element = (InstalledIUElement) selectionArray[i];
					int lock = getLock(profile, element.getIU());
					if ((lock & IInstallableUnit.LOCK_UNINSTALL) == IInstallableUnit.LOCK_UNINSTALL)
						return false;
					// If the parents are different, then they are either from 
					// different profiles or are nested in different parts of the tree.
					// Either way, this makes the selection invalid.
					if (parent == null) {
						parent = element.getParent(null);
					} else if (parent != element.getParent(null)) {
						return false;
					}
					// If it is not a visible IU, it is not uninstallable by the user
					String propName = getPolicy().getQueryContext().getVisibleInstalledIUProperty();
					if (propName != null && getProfileProperty(profile, element.getIU(), propName) == null) {
						return false;
					}
				} else {
					IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(selectionArray[i], IInstallableUnit.class);
					if (iu == null || !isSelectable(iu))
						return false;
				}
			}
			return true;
		}
		return false;
	}

	protected boolean isSelectable(IUElement element) {
		return super.isSelectable(element) && !(element.getParent(element) instanceof IUElement);
	}

	protected boolean isSelectable(IInstallableUnit iu) {
		if (!super.isSelectable(iu))
			return false;
		IProfile profile = getProfile(false);
		int lock = getLock(profile, iu);
		return ((lock & IInstallableUnit.LOCK_UNINSTALL) == IInstallableUnit.LOCK_NONE);
	}

	protected String getTaskName() {
		return ProvUIMessages.UninstallIUProgress;
	}

	protected int performAction(IInstallableUnit[] ius, String targetProfileId, ProvisioningPlan plan) {
		UninstallWizard wizard = new UninstallWizard(getPolicy(), targetProfileId, ius, plan);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
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
		} finally {
			sub.done();
		}
		return request;
	}

}
