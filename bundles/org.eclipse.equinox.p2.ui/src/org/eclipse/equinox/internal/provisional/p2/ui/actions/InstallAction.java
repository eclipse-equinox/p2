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
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.IUElement;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.InstallWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

public class InstallAction extends ProfileModificationAction {

	/**
	 * @deprecated temp hack for PDE UI
	 */
	public static ProvisioningPlan computeProvisioningPlan(IInstallableUnit[] ius, String targetProfileId, IProgressMonitor monitor) throws ProvisionException {
		ProfileChangeRequest request = computeProfileChangeRequest(ius, targetProfileId, PlanStatusHelper.getProfileChangeAlteredStatus(), monitor);
		return ProvisioningUtil.getProvisioningPlan(request, new ProvisioningContext(), monitor);
	}

	public static ProfileChangeRequest computeProfileChangeRequest(IInstallableUnit[] ius, String targetProfileId, MultiStatus status, IProgressMonitor monitor) {
		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(targetProfileId);
		IProfile profile;
		// Now check each individual IU for special cases
		try {
			profile = ProvisioningUtil.getProfile(targetProfileId);
		} catch (ProvisionException e) {
			status.add(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, e.getLocalizedMessage(), e));
			return null;
		}
		SubMonitor sub = SubMonitor.convert(monitor, ProvUIMessages.ProfileChangeRequestBuildingRequest, ius.length);
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
					status.merge(PlanStatusHelper.getStatus(IStatusCodes.IMPLIED_UPDATE, ius[i]));
				} else if (compareTo < 0) {
					// An implied downgrade.  We will not put this in the plan, add a status informing the user
					status.merge(PlanStatusHelper.getStatus(IStatusCodes.IGNORED_IMPLIED_DOWNGRADE, ius[i]));
				} else {
					if (Boolean.toString(true).equals(profile.getInstallableUnitProperty(installedIU, IInstallableUnit.PROP_PROFILE_ROOT_IU)))
						// It is already a root, nothing to do. We tell the user it was already installed
						status.merge(PlanStatusHelper.getStatus(IStatusCodes.IGNORED_ALREADY_INSTALLED, ius[i]));
					else
						// It was already installed but not as a root.  Nothing to tell the user, it will just seem like a fast install.
						request.setInstallableUnitProfileProperty(ius[i], IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
				}
			} else {
				// Install it and mark as a root
				request.addInstallableUnits(new IInstallableUnit[] {ius[i]});
				request.setInstallableUnitProfileProperty(ius[i], IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
			}
			sub.worked(1);
		}
		sub.done();
		return request;
	}

	public InstallAction(Policy policy, ISelectionProvider selectionProvider, String profileId) {
		super(policy, ProvUI.INSTALL_COMMAND_LABEL, selectionProvider, profileId);
		setToolTipText(ProvUI.INSTALL_COMMAND_TOOLTIP);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.actions.ProfileModificationAction#isEnabledFor(java.lang.Object[])
	 */
	protected boolean isEnabledFor(Object[] selectionArray) {
		if (selectionArray.length == 0)
			return false;
		// We allow non-IU's to be selected at this point, but there
		// must be at least one installable unit selected that is
		// not a category and is not nested underneath another IU.
		for (int i = 0; i < selectionArray.length; i++) {
			if (selectionArray[i] instanceof InstalledIUElement && isSelectable((IUElement) selectionArray[i]))
				return true;
		}
		return false;
	}

	/*
	 * Overridden to reject nested IU's
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.actions.ProfileModificationAction#isSelectable(org.eclipse.equinox.internal.p2.ui.model.IUElement)
	 */
	protected boolean isSelectable(IUElement element) {
		return super.isSelectable(element) && !(element.getParent(element) instanceof IUElement);
	}

	protected String getTaskName() {
		return ProvUIMessages.InstallIUProgress;
	}

	protected int performAction(IInstallableUnit[] ius, String targetProfileId, ProvisioningPlan plan) {
		InstallWizard wizard = new InstallWizard(getPolicy(), targetProfileId, ius, plan, new QueryableMetadataRepositoryManager(getPolicy(), false));
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.INSTALL_WIZARD);

		return dialog.open();
	}

	protected ProfileChangeRequest getProfileChangeRequest(IInstallableUnit[] ius, String targetProfileId, MultiStatus status, IProgressMonitor monitor) {
		return computeProfileChangeRequest(ius, targetProfileId, status, monitor);
	}
}
