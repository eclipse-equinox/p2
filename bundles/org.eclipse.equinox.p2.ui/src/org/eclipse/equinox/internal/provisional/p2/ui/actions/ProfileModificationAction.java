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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.CategoryElement;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

public abstract class ProfileModificationAction extends ProvisioningAction {
	public static final int ACTION_NOT_RUN = -1;
	String profileId;
	String userChosenProfileId;
	Policy policy;
	int result = ACTION_NOT_RUN;

	protected ProfileModificationAction(Policy policy, String text, ISelectionProvider selectionProvider, String profileId) {
		super(text, selectionProvider);
		this.policy = policy;
		this.profileId = profileId;
		init();
	}

	public void run() {
		// Determine which IUs and which profile are involved
		IInstallableUnit[] ius = getSelectedIUs();
		String id = getProfileId(true);
		// We could not figure out a profile to operate on, so return
		if (id == null || ius.length == 0) {
			ProvUI.reportStatus(getNoProfileOrSelectionStatus(profileId, ius), StatusManager.BLOCK);
			runCanceled();
			return;
		}
		run(ius, id);
	}

	protected IStatus getNoProfileOrSelectionStatus(String id, IInstallableUnit[] ius) {
		return new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, NLS.bind(ProvUIMessages.ProfileModificationAction_InvalidSelections, id, new Integer(ius.length)));
	}

	protected void run(final IInstallableUnit[] ius, final String id) {
		// Get a profile change request.  Supply a multi-status so that information
		// about the request can be provided along the way.
		final MultiStatus additionalStatus = getProfileChangeAlteredStatus();
		final ProfileChangeRequest[] request = new ProfileChangeRequest[1];
		// TODO even getting a profile change request can be expensive
		// when updating, because we are looking for updates.  For now, most
		// clients work around this by preloading repositories in a job.
		// Consider something different here.  We'll pass a fake progress monitor
		// into the profile change request method so that later we can do
		// something better here.
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				request[0] = getProfileChangeRequest(ius, id, additionalStatus, new NullProgressMonitor());
			}
		});
		// If we couldn't build a request, then report an error and bail.
		// Hopefully the provider of the request gave an explanation in the status.
		if (request[0] == null) {
			IStatus failureStatus;
			if (additionalStatus.getChildren().length > 0) {
				if (additionalStatus.getChildren().length == 1)
					failureStatus = additionalStatus.getChildren()[0];
				else {
					MultiStatus nullRequestStatus = new MultiStatus(ProvUIActivator.PLUGIN_ID, IStatusCodes.UNEXPECTED_NOTHING_TO_DO, additionalStatus.getChildren(), ProvUIMessages.ProfileModificationAction_NoChangeRequestProvided, null);
					nullRequestStatus.addAll(additionalStatus);
					failureStatus = nullRequestStatus;
				}
			} else {
				// No explanation for failure was provided.  It shouldn't happen, but...
				failureStatus = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProfileModificationAction_NoExplanationProvided);
			}
			ProvUI.reportStatus(failureStatus, StatusManager.SHOW | StatusManager.LOG);
			runCanceled();
			return;
		}
		// We have a profile change request, let's get a plan for it.  This could take awhile.
		final PlannerResolutionOperation operation = new PlannerResolutionOperation(ProvUIMessages.ProfileModificationAction_ResolutionOperationLabel, id, request[0], null, additionalStatus, isResolveUserVisible());
		// Since we are resolving asynchronously, our job is done.  Setting this allows
		// callers to decide to close the launching window.
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=236495
		result = Window.OK;
		Job job = ProvisioningOperationRunner.schedule(operation, StatusManager.SHOW);
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				// Do we have a plan??
				final ProvisioningPlan plan = operation.getProvisioningPlan();
				if (plan != null) {
					if (PlatformUI.isWorkbenchRunning()) {
						PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
							public void run() {
								if (validatePlan(plan))
									performAction(ius, getProfileId(true), operation);
								userChosenProfileId = null;
							}
						});
					}
				}
			}
		});
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
	protected abstract ProfileChangeRequest getProfileChangeRequest(IInstallableUnit[] ius, String targetProfileId, MultiStatus status, IProgressMonitor monitor);

	protected abstract int performAction(IInstallableUnit[] ius, String targetProfileId, PlannerResolutionOperation resolution);

	protected abstract String getTaskName();

	protected IInstallableUnit getIU(Object element) {
		return (IInstallableUnit) ProvUI.getAdapter(element, IInstallableUnit.class);

	}

	/**
	 * Return an array of the selected and valid installable units.
	 * The number of IInstallableUnits in the array may be different than
	 * the actual number of selections in the action's selection provider.
	 * That is, if the action is disabled due to invalid selections,
	 * this method will return those selections that were valid.
	 * 
	 * @return an array of selected IInstallableUnit that meet the
	 * enablement criteria for the action.  
	 */
	protected IInstallableUnit[] getSelectedIUs() {
		List elements = getStructuredSelection().toList();
		List iusList = new ArrayList(elements.size());

		for (int i = 0; i < elements.size(); i++) {
			if (elements.get(i) instanceof IIUElement) {
				IIUElement element = (IIUElement) elements.get(i);
				if (isSelectable(element))
					iusList.add(getIU(element));
			} else {
				IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(elements.get(i), IInstallableUnit.class);
				if (iu != null && isSelectable(iu))
					iusList.add(iu);
			}
		}
		return (IInstallableUnit[]) iusList.toArray(new IInstallableUnit[iusList.size()]);
	}

	protected boolean isSelectable(IIUElement element) {
		return !(element instanceof CategoryElement);
	}

	protected boolean isSelectable(IInstallableUnit iu) {
		return !ProvisioningUtil.isCategory(iu);
	}

	protected LicenseManager getLicenseManager() {
		return policy.getLicenseManager();
	}

	protected QueryProvider getQueryProvider() {
		return policy.getQueryProvider();
	}

	protected PlanValidator getPlanValidator() {
		return policy.getPlanValidator();
	}

	protected IProfileChooser getProfileChooser() {
		return policy.getProfileChooser();
	}

	protected Policy getPolicy() {
		return policy;
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

	protected String getProfileProperty(IProfile profile, IInstallableUnit iu, String propertyName) {
		if (profile == null || iu == null)
			return null;
		return profile.getInstallableUnitProperty(iu, propertyName);
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
		if (chooseProfile && getProfileChooser() != null) {
			userChosenProfileId = getProfileChooser().getProfileId(getShell());
			return userChosenProfileId;
		}
		return null;
	}

	private void runCanceled() {
		// The action was canceled, do any cleanup needed before
		// it is run again.
		userChosenProfileId = null;
		result = Window.CANCEL;
	}

	protected MultiStatus getProfileChangeAlteredStatus() {
		return PlanAnalyzer.getProfileChangeAlteredStatus();
	}

	protected boolean isResolveUserVisible() {
		return true;
	}
}
