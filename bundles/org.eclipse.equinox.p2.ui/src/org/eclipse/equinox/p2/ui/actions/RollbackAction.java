/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.ui.actions;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitConstants;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.equinox.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

public class RollbackAction extends ProfileModificationAction {

	public RollbackAction(ISelectionProvider selectionProvider, Profile profile, IProfileChooser chooser, Shell shell) {
		super(ProvUI.ROLLBACK_COMMAND_LABEL, selectionProvider, profile, chooser, shell);
		setToolTipText(ProvUI.ROLLBACK_COMMAND_TOOLTIP);
	}

	protected ProfileModificationOperation validateAndGetOperation(IInstallableUnit[] toBecome, Profile targetProfile, IProgressMonitor monitor, IAdaptable uiInfo) {
		if (toBecome.length == 1) {
			try {
				ProvisioningPlan plan = ProvisioningUtil.getBecomePlan(toBecome[0], targetProfile, monitor);
				IStatus planStatus = plan.getStatus();
				if (planStatus.isOK())
					return new ProfileModificationOperation(ProvUIMessages.RollbackIUOperationLabel, targetProfile.getProfileId(), plan);
				ProvUI.reportStatus(planStatus);
			} catch (ProvisionException e) {
				ProvUI.handleException(e, null);
				// fall through and return null
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 *
	 *  Overridden to enable only on single selections with a profile IU.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		Object[] selectionArray = selection.toArray();
		if (selectionArray.length == 1 && selectionArray[0] instanceof IInstallableUnit) {
			IInstallableUnit iu = (IInstallableUnit) selectionArray[0];
			setEnabled(Boolean.valueOf(iu.getProperty(IInstallableUnitConstants.PROFILE_IU_KEY)).booleanValue());
		} else {
			setEnabled(false);
		}
	}

	protected String getTaskName() {
		return ProvUIMessages.RollbackIUProgress;
	}

}