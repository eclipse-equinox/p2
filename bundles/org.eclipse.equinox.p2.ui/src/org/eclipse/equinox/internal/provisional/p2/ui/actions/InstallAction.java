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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.ui.PlanStatusHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.InstallWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policies;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

public class InstallAction extends ProfileModificationAction {

	public static ProvisioningPlan computeProvisioningPlan(IInstallableUnit[] ius, String targetProfileId, IProgressMonitor monitor) throws ProvisionException {
		MultiStatus additionalStatus = PlanStatusHelper.getProfileChangeAlteredStatus();
		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(targetProfileId);
		// Now check each individual IU for special cases
		IProfile profile = ProvisioningUtil.getProfile(targetProfileId);
		for (int i = 0; i < ius.length; i++) {
			// If the user is installing a patch, we mark it optional.  This allows
			// the patched IU to be updated later by removing the patch.
			if (Boolean.toString(true).equals(ius[i].getProperty(IInstallableUnit.PROP_TYPE_PATCH)))
				request.setInstallableUnitInclusionRules(ius[i], PlannerHelper.createOptionalInclusionRule(ius[i]));

			// Check to see if it is already installed.  This may alter the request.
			Collector alreadyInstalled = profile.query(new InstallableUnitQuery(ius[i].getId()), new Collector(), null);
			if (alreadyInstalled.size() > 0) {
				IInstallableUnit installedIU = (IInstallableUnit) alreadyInstalled.iterator().next();
				int compareTo = ius[i].getVersion().compareTo(installedIU.getVersion());
				// If the iu is a newer version of something already installed, consider this an
				// update request
				if (compareTo > 0) {
					request.addInstallableUnits(new IInstallableUnit[] {ius[i]});
					request.removeInstallableUnits(new IInstallableUnit[] {installedIU});
					// Mark it as a root if it hasn't been already
					if (!Boolean.toString(true).equals(profile.getInstallableUnitProperty(installedIU, IInstallableUnit.PROP_PROFILE_ROOT_IU)))
						request.setInstallableUnitProfileProperty(ius[i], IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
					// Add a status informing the user that the update has been inferred
					additionalStatus.merge(PlanStatusHelper.getStatus(IStatusCodes.IMPLIED_UPDATE, ius[i]));
				} else if (compareTo < 0) {
					// An implied downgrade.  We will not put this in the plan, add a status informing the user
					additionalStatus.merge(PlanStatusHelper.getStatus(IStatusCodes.IGNORED_IMPLIED_DOWNGRADE, ius[i]));
				} else {
					if (Boolean.toString(true).equals(profile.getInstallableUnitProperty(installedIU, IInstallableUnit.PROP_PROFILE_ROOT_IU)))
						// It is already a root, nothing to do. We tell the user it was already installed
						additionalStatus.merge(PlanStatusHelper.getStatus(IStatusCodes.IGNORED_ALREADY_INSTALLED, ius[i]));
					else
						// It was already installed but not as a root.  Nothing to tell the user, it will just seem like a fast install.
						request.setInstallableUnitProfileProperty(ius[i], IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
				}
			} else {
				// Install it and mark as a root
				request.addInstallableUnits(new IInstallableUnit[] {ius[i]});
				request.setInstallableUnitProfileProperty(ius[i], IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
			}
		}
		// Now that we know what we are requesting, get the plan
		ProvisioningPlan plan = ProvisioningUtil.getProvisioningPlan(request, new ProvisioningContext(), monitor);

		// If we recorded additional status along the way, build a plan that merges in this status.
		// Ideally this all would have been detected in the planner itself.
		if (additionalStatus.getChildren().length > 0) {
			additionalStatus.merge(plan.getStatus());
			plan = new ProvisioningPlan(additionalStatus, plan.getOperands());
		}
		// Now run the result through the sanity checker.  Again, this would ideally be caught
		// in the planner, but for now we have to build a new plan to include the UI status checking.
		return new ProvisioningPlan(PlanStatusHelper.computeStatus(plan, ius), plan.getOperands());
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
		return selectionArray.length > 0;
	}

	protected String getTaskName() {
		return ProvUIMessages.InstallIUProgress;
	}

	protected int performOperation(IInstallableUnit[] ius, String targetProfileId, ProvisioningPlan plan) {
		InstallWizard wizard = new InstallWizard(targetProfileId, ius, plan, getLicenseManager());
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		return dialog.open();
	}

	protected ProvisioningPlan getProvisioningPlan(IInstallableUnit[] ius, String targetProfileId, IProgressMonitor monitor) throws ProvisionException {
		return computeProvisioningPlan(ius, targetProfileId, monitor);
	}
}
