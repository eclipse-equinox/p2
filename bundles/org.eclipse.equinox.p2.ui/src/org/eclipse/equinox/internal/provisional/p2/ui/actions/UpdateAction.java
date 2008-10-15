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

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.PlanStatusHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.AvailableUpdateElement;
import org.eclipse.equinox.internal.p2.ui.model.IUElement;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.UpdateWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.internal.provisional.p2.ui.model.Updates;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

public class UpdateAction extends ProfileModificationAction {

	ArrayList allReplacements; // cache all the replacements found to seed the wizard
	HashMap latestReplacements;
	boolean resolveIsVisible = true;

	public UpdateAction(Policy policy, ISelectionProvider selectionProvider, String profileId, boolean resolveIsVisible) {
		super(policy, ProvUI.UPDATE_COMMAND_LABEL, selectionProvider, profileId);
		setToolTipText(ProvUI.UPDATE_COMMAND_TOOLTIP);
		this.resolveIsVisible = resolveIsVisible;
	}

	protected int performAction(IInstallableUnit[] ius, String targetProfileId, ProvisioningPlan plan) {
		// Caches should have been created while formulating the plan
		Assert.isNotNull(latestReplacements);
		Assert.isNotNull(allReplacements);
		Assert.isNotNull(plan);

		UpdateWizard wizard = new UpdateWizard(getPolicy(), targetProfileId, ius, (AvailableUpdateElement[]) allReplacements.toArray(new AvailableUpdateElement[allReplacements.size()]), latestReplacements.values().toArray(), plan, getLicenseManager());
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.UPDATE_WIZARD);

		return dialog.open();
	}

	protected ProfileChangeRequest getProfileChangeRequest(IInstallableUnit[] ius, String targetProfileId, MultiStatus status, IProgressMonitor monitor) {
		// Here we create a profile change request by finding the latest version available for any replacement.
		ArrayList toBeUpdated = new ArrayList();
		latestReplacements = new HashMap();
		allReplacements = new ArrayList();
		SubMonitor sub = SubMonitor.convert(monitor, ProvUIMessages.ProfileChangeRequestBuildingRequest, ius.length);
		for (int i = 0; i < ius.length; i++) {
			ElementQueryDescriptor descriptor = getQueryProvider().getQueryDescriptor(new Updates(targetProfileId, new IInstallableUnit[] {ius[i]}));
			Iterator iter = descriptor.queryable.query(descriptor.query, descriptor.collector, sub).iterator();
			if (iter.hasNext())
				toBeUpdated.add(ius[i]);
			ArrayList currentReplacements = new ArrayList();
			while (iter.hasNext()) {
				IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(iter.next(), IInstallableUnit.class);
				if (iu != null) {
					AvailableUpdateElement element = new AvailableUpdateElement(null, iu, ius[i], targetProfileId);
					currentReplacements.add(element);
					allReplacements.add(element);
				}
			}
			for (int j = 0; j < currentReplacements.size(); j++) {
				AvailableUpdateElement replacementElement = (AvailableUpdateElement) currentReplacements.get(j);
				AvailableUpdateElement latestElement = (AvailableUpdateElement) latestReplacements.get(replacementElement.getIU().getId());
				IInstallableUnit latestIU = latestElement == null ? null : latestElement.getIU();
				if (latestIU == null || replacementElement.getIU().getVersion().compareTo(latestIU.getVersion()) > 0)
					latestReplacements.put(replacementElement.getIU().getId(), replacementElement);
			}
			sub.worked(1);
		}
		if (toBeUpdated.size() <= 0) {
			status.add(PlanStatusHelper.getStatus(IStatusCodes.NOTHING_TO_UPDATE, null));
			sub.done();
			return null;
		}

		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(targetProfileId);
		Iterator iter = toBeUpdated.iterator();
		while (iter.hasNext())
			request.removeInstallableUnits(new IInstallableUnit[] {(IInstallableUnit) iter.next()});
		iter = latestReplacements.values().iterator();
		while (iter.hasNext())
			request.addInstallableUnits(new IInstallableUnit[] {((AvailableUpdateElement) iter.next()).getIU()});
		sub.done();
		return request;
	}

	protected boolean isEnabledFor(Object[] selectionArray) {
		Object parent = null;
		// We cache the profile for performance reasons rather than get it for
		// each IU.  
		IProfile profile = getProfile(false);
		if (profile == null)
			return false;
		if (selectionArray.length > 0) {
			for (int i = 0; i < selectionArray.length; i++) {
				if (selectionArray[i] instanceof InstalledIUElement) {
					InstalledIUElement element = (InstalledIUElement) selectionArray[i];
					int lock = getLock(profile, element.getIU());
					// If it is locked for update, action is not allowed
					if ((lock & IInstallableUnit.LOCK_UPDATE) == IInstallableUnit.LOCK_UPDATE)
						return false;
					// We reject any selection with different parents,
					// so if there were IU's selected from multiple
					// profiles, we catch this case and disable the action.
					if (parent == null) {
						parent = element.getParent(null);
					} else if (parent != element.getParent(null)) {
						return false;
					}
					// If it is not a visible IU, it is not updatable by the user
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
		return ((lock & IInstallableUnit.LOCK_UPDATE) == IInstallableUnit.LOCK_NONE);
	}

	protected String getTaskName() {
		return ProvUIMessages.UpdateIUProgress;
	}

	protected boolean isResolveUserVisible() {
		return resolveIsVisible;
	}
}
