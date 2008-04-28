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

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

public abstract class ProfileModificationAction extends ProvisioningAction {

	private String profileId;
	IProfileChooser profileChooser;
	Policies policies;

	protected ProfileModificationAction(String text, ISelectionProvider selectionProvider, String profileId, IProfileChooser profileChooser, Policies policies, Shell shell) {
		super(text, selectionProvider, shell);
		this.profileId = profileId;
		this.profileChooser = profileChooser;
		this.policies = policies;
		init();
	}

	protected ProvisioningPlan getProvisioningPlan() {
		final String id = getProfileId();
		// We could not figure out a profile to operate on, so return
		if (id == null) {
			return null;
		}

		final IInstallableUnit[] ius = getSelectedIUs();
		final ProvisioningPlan[] plan = new ProvisioningPlan[1];
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
		return plan[0];
	}

	public void run() {
		ProvisioningPlan plan = getProvisioningPlan();
		if (validatePlan(plan))
			performOperation(getSelectedIUs(), getProfileId(), plan);
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
			if (getPlanValidator() != null)
				return getPlanValidator().continueWorkingWithPlan(plan, getShell());
			if (plan.getStatus().isOK())
				return true;
			ProvUI.reportStatus(plan.getStatus(), StatusManager.BLOCK | StatusManager.LOG);
			return false;
		}
		return false;
	}

	/*
	 * Get a provisioning plan for this action.
	 */
	protected abstract ProvisioningPlan getProvisioningPlan(IInstallableUnit[] ius, String targetProfileId, IProgressMonitor monitor) throws ProvisionException;

	protected abstract void performOperation(IInstallableUnit[] ius, String targetProfileId, ProvisioningPlan plan);

	protected abstract String getTaskName();

	protected IInstallableUnit[] getSelectedIUs() {
		return ElementUtils.getIUs(getStructuredSelection().toArray());
	}

	protected LicenseManager getLicenseManager() {
		return policies.getLicenseManager();
	}

	protected IQueryProvider getQueryProvider() {
		return policies.getQueryProvider();
	}

	protected IPlanValidator getPlanValidator() {
		return policies.getPlanValidator();
	}

	protected Policies getPolicies() {
		return policies;
	}

	protected final void checkEnablement(Object[] selections) {
		setEnabled(isEnabledFor(selections) && !ProvisioningOperationRunner.hasScheduledOperationsFor(getProfileId()));
	}

	protected abstract boolean isEnabledFor(Object[] selections);

	protected int getLock(IProfile profile, IInstallableUnit iu) {
		if (profile == null)
			return IInstallableUnit.LOCK_NONE;
		try {
			String value = profile.getInstallableUnitProperty(iu, IInstallableUnit.PROP_PROFILE_LOCKED_IU);
			if (value != null)
				return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			// ignore and assume no lock
		}
		return IInstallableUnit.LOCK_NONE;
	}

	protected IProfile getProfile() {
		try {
			String id = getProfileId();
			if (id == null)
				return null;
			return ProvisioningUtil.getProfile(id);
		} catch (ProvisionException e) {
			// ignore, we have bigger problems to report elsewhere
		}
		return null;
	}

	protected String getProfileId() {
		if (profileId != null)
			return profileId;
		if (profileChooser != null)
			return profileChooser.getProfileId(getShell());
		return null;
	}
}
