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
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerHelper;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.PreselectedIUInstallWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.ProvisioningWizardDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

public class InstallAction extends ProfileModificationAction {

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
			// TODO ideally we should only do this check if the iu is a singleton, but in practice many iu's that should
			// be singletons are not, so we don't check this (yet)
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=230878
			if (alreadyInstalled.size() > 0) { //  && installedIU.isSingleton()
				IInstallableUnit installedIU = (IInstallableUnit) alreadyInstalled.iterator().next();
				int compareTo = ius[i].getVersion().compareTo(installedIU.getVersion());
				// If the iu is a newer version of something already installed, consider this an
				// update request
				if (compareTo > 0) {
					boolean lockedForUpdate = false;
					String value = profile.getInstallableUnitProperty(installedIU, IInstallableUnit.PROP_PROFILE_LOCKED_IU);
					if (value != null)
						lockedForUpdate = (Integer.parseInt(value) & IInstallableUnit.LOCK_UPDATE) == IInstallableUnit.LOCK_UPDATE;
					if (lockedForUpdate) {
						// Add a status telling the user that this implies an update, but the
						// iu should not be updated
						status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_IGNORED_IMPLIED_UPDATE, ius[i]));
					} else {
						request.addInstallableUnits(new IInstallableUnit[] {ius[i]});
						request.removeInstallableUnits(new IInstallableUnit[] {installedIU});
						// Add a status informing the user that the update has been inferred
						status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_IMPLIED_UPDATE, ius[i]));
						// Mark it as a root if it hasn't been already
						if (!Boolean.toString(true).equals(profile.getInstallableUnitProperty(installedIU, IInstallableUnit.PROP_PROFILE_ROOT_IU)))
							request.setInstallableUnitProfileProperty(ius[i], IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
					}
				} else if (compareTo < 0) {
					// An implied downgrade.  We will not put this in the plan, add a status informing the user
					status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_IGNORED_IMPLIED_DOWNGRADE, ius[i]));
				} else {
					if (Boolean.toString(true).equals(profile.getInstallableUnitProperty(installedIU, IInstallableUnit.PROP_PROFILE_ROOT_IU)))
						// It is already a root, nothing to do. We tell the user it was already installed
						status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_IGNORED_ALREADY_INSTALLED, ius[i]));
					else {
						// It was already installed but not as a root.  Tell the user that parts of it are already installed and mark
						// it as a root. 
						status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_PARTIAL_INSTALL, ius[i]));
						request.setInstallableUnitProfileProperty(ius[i], Policy.getDefault().getQueryContext().getVisibleInstalledIUProperty(), Boolean.toString(true));
					}
				}
			} else {
				// Install it and mark as a root
				request.addInstallableUnits(new IInstallableUnit[] {ius[i]});
				request.setInstallableUnitProfileProperty(ius[i], Policy.getDefault().getQueryContext().getVisibleInstalledIUProperty(), Boolean.toString(true));
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
		// selectable
		for (int i = 0; i < selectionArray.length; i++) {
			if (selectionArray[i] instanceof InstalledIUElement && isSelectable((IIUElement) selectionArray[i]))
				return true;
			IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(selectionArray[i], IInstallableUnit.class);
			if (iu != null && isSelectable(iu))
				return true;
		}
		return false;
	}

	/*
	 * Overridden to reject categories and nested IU's (parent is a non-category IU)
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.actions.ProfileModificationAction#isSelectable(org.eclipse.equinox.internal.p2.ui.model.IUElement)
	 */
	protected boolean isSelectable(IIUElement element) {
		return super.isSelectable(element) && !(element.getParent(element) instanceof AvailableIUElement);
	}

	protected String getTaskName() {
		return ProvUIMessages.InstallIUProgress;
	}

	protected int performAction(IInstallableUnit[] ius, String targetProfileId, PlannerResolutionOperation resolution) {
		PreselectedIUInstallWizard wizard = new PreselectedIUInstallWizard(getPolicy(), targetProfileId, ius, resolution, new QueryableMetadataRepositoryManager(getPolicy().getQueryContext(), false));
		WizardDialog dialog = new ProvisioningWizardDialog(getShell(), wizard);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.INSTALL_WIZARD);

		return dialog.open();
	}

	protected ProfileChangeRequest getProfileChangeRequest(IInstallableUnit[] ius, String targetProfileId, MultiStatus status, IProgressMonitor monitor) {
		return computeProfileChangeRequest(ius, targetProfileId, status, monitor);
	}
}
