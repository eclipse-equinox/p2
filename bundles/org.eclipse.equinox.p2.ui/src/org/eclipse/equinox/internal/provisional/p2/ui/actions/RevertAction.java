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

import java.util.Set;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policies;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;

public class RevertAction extends ProfileModificationAction {

	public RevertAction(ISelectionProvider selectionProvider, String profileId, IProfileChooser chooser, Policies policies, Shell shell) {
		super(ProvUI.REVERT_COMMAND_LABEL, selectionProvider, profileId, chooser, policies, shell);
		setToolTipText(ProvUI.REVERT_COMMAND_TOOLTIP);
	}

	protected ProvisioningPlan getProvisioningPlan(IInstallableUnit[] toRevert, String targetProfileId, IProgressMonitor monitor) throws ProvisionException {
		if (toRevert.length == 1)
			return ProvisioningUtil.getRevertPlan(toRevert[0], monitor);
		// should never happen
		return null;
	}

	protected void performOperation(IInstallableUnit[] toBecome, String targetProfileId, ProvisioningPlan plan) {
		ProvisioningOperation op = new ProfileModificationOperation(ProvUIMessages.RevertIUOperationLabel, targetProfileId, plan);
		ProvisioningOperationRunner.schedule(op, getShell());
	}

	/*
	 * Overridden to only allow selection of single rollback IU's
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.actions.ProfileModificationAction#isEnabledFor(java.lang.Object[])
	 */
	protected boolean isEnabledFor(Object[] selectionArray) {
		if (selectionArray.length == 1) {
			Set ius = ElementUtils.getIUs(selectionArray[0]);
			if (ius.size() == 1) {
				Object element = ius.iterator().next();
				if (element instanceof IInstallableUnit)
					return Boolean.valueOf(((IInstallableUnit) element).getProperty(IInstallableUnit.PROP_TYPE_PROFILE)).booleanValue();
			}
		}
		return false;
	}

	protected String getTaskName() {
		return ProvUIMessages.RevertIUProgress;
	}

}
