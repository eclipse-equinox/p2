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

import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.ui.IProfileChooser;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.InstallWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policies;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

public class InstallAction extends ProfileModificationAction {

	public static ProvisioningPlan computeProvisioningPlan(IInstallableUnit[] ius, String targetProfileId, IProgressMonitor monitor) throws ProvisionException {
		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(targetProfileId);
		request.addInstallableUnits(ius);
		IProfile profile = ProvisioningUtil.getProfile(targetProfileId);
		IInstallableUnit[] installedIUs = (IInstallableUnit[]) profile.query(new InstallableUnitQuery(null), new Collector(), null).toArray(IInstallableUnit.class);
		for (int i = 0; i < ius.length; i++) {
			// If the user is installing a patch, we mark it optional.  This allows
			// the patched IU to be updated later by removing the patch.
			if (Boolean.toString(true).equals(ius[i].getProperty(IInstallableUnit.PROP_TYPE_PATCH)))
				request.setInstallableUnitInclusionRules(ius[i], PlannerHelper.createOptionalInclusionRule(ius[i]));
			// If the iu is replacing something already installed, request the removal of the one in the profile
			for (int j = 0; j < installedIUs.length; j++)
				if (installedIUs[j].getId().equals(ius[i].getId())) {
					request.removeInstallableUnits(new IInstallableUnit[] {installedIUs[j]});
					break;
				}
			// Mark everything we are installing as a root
			request.setInstallableUnitProfileProperty(ius[i], IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
		}
		ProvisioningPlan plan = ProvisioningUtil.getProvisioningPlan(request, new ProvisioningContext(), monitor);
		return plan;
	}

	public InstallAction(ISelectionProvider selectionProvider, String profileId, IProfileChooser chooser, Policies policies, Shell shell) {
		super(ProvUI.INSTALL_COMMAND_LABEL, selectionProvider, profileId, chooser, policies, shell);
		setToolTipText(ProvUI.INSTALL_COMMAND_TOOLTIP);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.actions.ProfileModificationAction#isEnabledFor(java.lang.Object[])
	 */
	protected boolean isEnabledFor(Object[] selectionArray) {
		if (selectionArray.length < 1)
			return false;
		Set children = new HashSet();

		for (int i = 0; i < selectionArray.length; i++) {
			children.addAll(ElementUtils.getIUs(selectionArray[i]));
		}
		return !children.isEmpty();
	}

	protected String getTaskName() {
		return ProvUIMessages.InstallIUProgress;
	}

	protected void performOperation(IInstallableUnit[] ius, String targetProfileId, ProvisioningPlan plan) {
		InstallWizard wizard = new InstallWizard(targetProfileId, ius, plan, getLicenseManager());
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.open();
	}

	protected ProvisioningPlan getProvisioningPlan(IInstallableUnit[] ius, String targetProfileId, IProgressMonitor monitor) throws ProvisionException {
		return computeProvisioningPlan(ius, targetProfileId, monitor);
	}
}
