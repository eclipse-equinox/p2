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

package org.eclipse.equinox.internal.p2.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

public abstract class ProfileModificationAction extends ProvisioningAction {

	String profileId;
	IProfileChooser profileChooser;
	LicenseManager licenseManager;

	protected ProfileModificationAction(String text, ISelectionProvider selectionProvider, String profileId, IProfileChooser profileChooser, LicenseManager licenseManager, Shell shell) {
		super(text, selectionProvider, shell);
		this.profileId = profileId;
		this.profileChooser = profileChooser;
		this.licenseManager = licenseManager;
	}

	public void run() {
		// If the profile was not provided, see if we have a
		// viewer element that can tell us.
		String targetProfileId = profileId;
		if (targetProfileId == null && profileChooser != null) {
			targetProfileId = profileChooser.getProfileId(getShell());
		}
		// We could not figure out a profile to operate on, so return
		if (targetProfileId == null) {
			return;
		}

		List elements = getStructuredSelection().toList();
		List iusList = new ArrayList(elements.size());

		for (int i = 0; i < elements.size(); i++) {
			IInstallableUnit iu = getIU(elements.get(i));
			if (iu != null)
				iusList.add(iu);
		}

		final IInstallableUnit[] ius = (IInstallableUnit[]) iusList.toArray(new IInstallableUnit[iusList.size()]);
		final ProvisioningPlan[] plan = new ProvisioningPlan[1];
		final String id = targetProfileId;
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
					plan[0] = getProvisioningPlan(ius, id, monitor);
				} catch (ProvisionException e) {
					ProvUI.handleException(e, ProvUIMessages.ProfileModificationAction_UnexpectedError, StatusManager.BLOCK | StatusManager.LOG);
				}
			}
		};
		try {
			new ProgressMonitorDialog(getShell()).run(true, true, runnable);
		} catch (InterruptedException e) {
			// don't report thread interruption
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), ProvUIMessages.ProfileModificationAction_UnexpectedError, StatusManager.BLOCK | StatusManager.LOG);
		}

		if (validatePlan(plan[0]))
			performOperation(ius, id, plan[0]);

	}

	/**
	 * Validate the plan and return true if the operation should
	 * be performed with plan.  Report any errors to the user before returning false.
	 * @param plan
	 * @return a boolean indicating whether the plan should be used in a
	 * provisioning operation.
	 */
	protected boolean validatePlan(ProvisioningPlan plan) {
		if (plan != null) {
			if (plan.getStatus().isOK())
				return true;
			// TODO give user option to continue anyway to wizard
			ProvUI.reportStatus(plan.getStatus(), StatusManager.BLOCK | StatusManager.LOG);
			return false;
		}
		// plan was null, no exception thrown.  
		ProvUI.reportStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProfileModificationAction_NullPlan), StatusManager.BLOCK | StatusManager.LOG);
		return false;
	}

	/*
	 * Get a provisioning plan for this action.
	 */
	protected abstract ProvisioningPlan getProvisioningPlan(IInstallableUnit[] ius, String targetProfileId, IProgressMonitor monitor) throws ProvisionException;

	protected abstract void performOperation(IInstallableUnit[] ius, String targetProfileId, ProvisioningPlan plan);

	protected abstract String getTaskName();

	protected IInstallableUnit getIU(Object element) {
		return (IInstallableUnit) ProvUI.getAdapter(element, IInstallableUnit.class);

	}

	protected LicenseManager getLicenseManager() {
		return licenseManager;
	}

}
