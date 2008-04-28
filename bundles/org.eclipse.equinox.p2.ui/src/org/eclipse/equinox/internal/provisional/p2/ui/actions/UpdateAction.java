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
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.AvailableUpdateElement;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.UpdateWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policies;
import org.eclipse.equinox.internal.provisional.p2.ui.query.ElementQueryDescriptor;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.UpdateEvent;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

public class UpdateAction extends ProfileModificationAction {

	ArrayList allReplacements; // cache all the replacements found to seed the wizard
	HashMap latestReplacements;

	public UpdateAction(ISelectionProvider selectionProvider, String profileId, IProfileChooser chooser, Policies policies, Shell shell) {
		super(ProvUI.UPDATE_COMMAND_LABEL, selectionProvider, profileId, chooser, policies, shell);
		setToolTipText(ProvUI.UPDATE_COMMAND_TOOLTIP);
	}

	protected void performOperation(IInstallableUnit[] ius, String targetProfileId, ProvisioningPlan plan) {
		// Caches should have been created while formulating the plan
		Assert.isNotNull(latestReplacements);
		Assert.isNotNull(allReplacements);
		Assert.isNotNull(plan);

		UpdateWizard wizard = new UpdateWizard(targetProfileId, ius, (AvailableUpdateElement[]) allReplacements.toArray(new AvailableUpdateElement[allReplacements.size()]), latestReplacements.values().toArray(), plan, getLicenseManager());
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.open();
	}

	protected ProvisioningPlan getProvisioningPlan(IInstallableUnit[] ius, String targetProfileId, IProgressMonitor monitor) throws ProvisionException {
		// Here we create a provisioning plan by finding the latest version available for any replacement.
		// TODO to be smarter, we could check older versions if a new version made a plan invalid.
		ArrayList toBeUpdated = new ArrayList();
		latestReplacements = new HashMap();
		allReplacements = new ArrayList();
		for (int i = 0; i < ius.length; i++) {
			UpdateEvent event = new UpdateEvent(targetProfileId, new IInstallableUnit[] {ius[i]});
			ElementQueryDescriptor descriptor = getQueryProvider().getQueryDescriptor(event, IQueryProvider.AVAILABLE_UPDATES);
			Iterator iter = descriptor.queryable.query(descriptor.query, descriptor.collector, null).iterator();
			if (iter.hasNext())
				toBeUpdated.add(ius[i]);
			ArrayList currentReplacements = new ArrayList();
			while (iter.hasNext()) {
				IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(iter.next(), IInstallableUnit.class);
				if (iu != null) {
					AvailableUpdateElement element = new AvailableUpdateElement(iu, ius[i], targetProfileId);
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
		}
		if (toBeUpdated.size() <= 0) {
			return new ProvisioningPlan(new Status(IStatus.INFO, ProvUIActivator.PLUGIN_ID, IStatusCodes.NOTHING_TO_UPDATE, ProvUIMessages.UpdateOperation_NothingToUpdate, null));
		}

		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(targetProfileId);
		Iterator iter = toBeUpdated.iterator();
		while (iter.hasNext())
			request.removeInstallableUnits(new IInstallableUnit[] {(IInstallableUnit) iter.next()});
		iter = latestReplacements.values().iterator();
		while (iter.hasNext())
			request.addInstallableUnits(new IInstallableUnit[] {((AvailableUpdateElement) iter.next()).getIU()});
		ProvisioningPlan plan = ProvisioningUtil.getProvisioningPlan(request, new ProvisioningContext(), monitor);
		return plan;
	}

	protected boolean isEnabledFor(Object[] selectionArray) {
		Object parent = null;
		// We cache the profile for performance reasons rather than get it for
		// each IU.  Note that below we reject any selection
		// with different parents, so if there were IU's selected from multiple
		// profiles, we catch this case and disable the action.
		IProfile profile = getProfile();
		if (profile == null)
			return false;
		if (selectionArray.length > 0) {
			for (int i = 0; i < selectionArray.length; i++) {
				if (selectionArray[i] instanceof InstalledIUElement) {
					InstalledIUElement element = (InstalledIUElement) selectionArray[i];
					int lock = getLock(profile, element.getIU());
					if ((lock & IInstallableUnit.LOCK_UPDATE) == IInstallableUnit.LOCK_UPDATE)
						return false;
					if (parent == null) {
						parent = element.getParent(null);
					} else if (parent != element.getParent(null)) {
						return false;
					}
				} else {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	protected String getTaskName() {
		return ProvUIMessages.UpdateIUProgress;
	}
}
