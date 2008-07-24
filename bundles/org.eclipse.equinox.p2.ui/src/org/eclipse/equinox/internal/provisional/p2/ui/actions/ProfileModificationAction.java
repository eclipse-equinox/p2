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
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

public abstract class ProfileModificationAction extends ProvisioningAction {
	public static final int ACTION_NOT_RUN = -1;
	private String profileId;
	private String userChosenProfileId;
	IProfileChooser profileChooser;
	Policies policies;
	int result = ACTION_NOT_RUN;

	protected ProfileModificationAction(String text, ISelectionProvider selectionProvider, String profileId, IProfileChooser profileChooser, Policies policies, Shell shell) {
		super(text, selectionProvider, shell);
		this.profileId = profileId;
		this.profileChooser = profileChooser;
		this.policies = policies;
		init();
	}

	protected ProvisioningPlan getProvisioningPlan() {
		final String id = getProfileId(true);
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
			result = performOperation(getSelectedIUs(), getProfileId(true), plan);
		else
			result = Window.CANCEL;
		userChosenProfileId = null;
	}

	/**
	 * Get the integer return code returned by any wizards launched by this
	 * action.  If the action has not been run, return ACTION_NOT_RUN.  If the
	 * action does not open a wizard, return Window.OK if the operation was performed,
	 * and Window.CANCEL if it was canceled.
	 * 
	 * @return integer return code
	 */
	public int getReturnCode() {
		return result;
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
			// Don't validate the plan if the user cancelled
			if (plan.getStatus().getSeverity() == IStatus.CANCEL)
				return false;
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

	protected abstract int performOperation(IInstallableUnit[] ius, String targetProfileId, ProvisioningPlan plan);

	protected abstract String getTaskName();

	protected IInstallableUnit getIU(Object element) {
		return (IInstallableUnit) ProvUI.getAdapter(element, IInstallableUnit.class);

	}

	protected IInstallableUnit[] getSelectedIUs() {
		List elements = getStructuredSelection().toList();
		List iusList = new ArrayList(elements.size());

		for (int i = 0; i < elements.size(); i++) {
			IInstallableUnit iu = getIU(elements.get(i));
			if (iu != null && !ProvisioningUtil.isCategory(iu))
				iusList.add(iu);
		}

		return (IInstallableUnit[]) iusList.toArray(new IInstallableUnit[iusList.size()]);
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
		if (isEnabledFor(selections)) {
			String id = getProfileId(false);
			if (id == null)
				setEnabled(true);
			else
				setEnabled(!ProvisioningOperationRunner.hasScheduledOperationsFor(id));
		} else
			setEnabled(false);
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

	protected IProfile getProfile(boolean chooseProfile) {
		try {
			String id = getProfileId(chooseProfile);
			if (id == null)
				return null;
			return ProvisioningUtil.getProfile(id);
		} catch (ProvisionException e) {
			// ignore, we have bigger problems to report elsewhere
		}
		return null;
	}

	protected String getProfileId(boolean chooseProfile) {
		if (profileId != null)
			return profileId;
		if (userChosenProfileId != null)
			return userChosenProfileId;
		if (chooseProfile && profileChooser != null) {
			userChosenProfileId = profileChooser.getProfileId(getShell());
			return userChosenProfileId;
		}
		return null;
	}
}
