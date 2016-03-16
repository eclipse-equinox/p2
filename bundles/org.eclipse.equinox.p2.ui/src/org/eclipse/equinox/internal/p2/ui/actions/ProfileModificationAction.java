/*******************************************************************************
 *  Copyright (c) 2007, 2016 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.actions;

import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.CategoryElement;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.ui.LicenseManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

public abstract class ProfileModificationAction extends ProvisioningAction {
	public static final int ACTION_NOT_RUN = -1;
	String profileId;
	int result = ACTION_NOT_RUN;

	protected ProfileModificationAction(ProvisioningUI ui, String text, ISelectionProvider selectionProvider, String profileId) {
		super(ui, text, selectionProvider);
		this.ui = ui;
		this.profileId = profileId;
		init();
	}

	public void run() {
		Collection<IInstallableUnit> ius = getSelectedIUs();
		// No ius or no profile?
		if (isInvalidProfileId() || ius.size() == 0) {
			ProvUI.reportStatus(getNoProfileOrSelectionStatus(profileId, ius), StatusManager.BLOCK);
			runCanceled();
			return;
		}
		run(ius, profileId);
	}

	protected boolean isInvalidProfileId() {
		return profileId == null;
	}

	public IProfile getProfile() {
		String id = profileId == null ? ui.getProfileId() : profileId;
		return ProvUI.getProfileRegistry(ui.getSession()).getProfile(id);
	}

	protected IStatus getNoProfileOrSelectionStatus(String id, Collection<IInstallableUnit> ius) {
		return new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, NLS.bind(ProvUIMessages.ProfileModificationAction_InvalidSelections, id, Integer.valueOf(ius.size())));
	}

	protected abstract ProfileChangeOperation getProfileChangeOperation(Collection<IInstallableUnit> ius);

	protected void run(final Collection<IInstallableUnit> ius, final String id) {
		final ProfileChangeOperation operation = getProfileChangeOperation(ius);
		ProvisioningJob job = operation.getResolveJob(null);
		if (job == null) {
			ProvUI.reportStatus(operation.getResolutionResult(), StatusManager.SHOW);
		} else {
			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {

					if (PlatformUI.isWorkbenchRunning()) {
						PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
							public void run() {
								if (validateOperation(operation))
									performAction(operation, ius);
							}
						});
					}

				}

			});
			// Don't show the error dialog anymore since the remediation operation should be computed
			getProvisioningUI().schedule(job, StatusManager.NONE | StatusManager.LOG);
		}
		// Since we are resolving asynchronously, our job is done.  Setting this allows
		// callers to decide to close the launching window.
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=236495
		result = Window.OK;
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
	 * Validate the operation and return true if the operation should
	 * be performed with plan.  Report any errors to the user before returning false.
	 * @param operation
	 * @return a boolean indicating whether the operation should be used in a
	 * provisioning operation.
	 */
	protected boolean validateOperation(ProfileChangeOperation operation) {
		if (operation != null) {
			return getPolicy().continueWorkingWithOperation(operation, getShell());
		}
		return false;
	}

	protected abstract int performAction(ProfileChangeOperation operation, Collection<IInstallableUnit> ius);

	protected IInstallableUnit getIU(Object element) {
		return ProvUI.getAdapter(element, IInstallableUnit.class);

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
	protected List<IInstallableUnit> getSelectedIUs() {
		List<?> elements = getStructuredSelection().toList();
		List<IInstallableUnit> iusList = new ArrayList<IInstallableUnit>(elements.size());

		for (int i = 0; i < elements.size(); i++) {
			if (elements.get(i) instanceof IIUElement) {
				IIUElement element = (IIUElement) elements.get(i);
				if (isSelectable(element))
					iusList.add(getIU(element));
			} else {
				IInstallableUnit iu = ProvUI.getAdapter(elements.get(i), IInstallableUnit.class);
				if (iu != null && isSelectable(iu))
					iusList.add(iu);
			}
		}
		return iusList;
	}

	protected boolean isSelectable(IIUElement element) {
		return !(element instanceof CategoryElement);
	}

	protected boolean isSelectable(IInstallableUnit iu) {
		return !ProvUI.isCategory(iu);
	}

	protected LicenseManager getLicenseManager() {
		return getProvisioningUI().getLicenseManager();
	}

	protected final void checkEnablement(Object[] selections) {
		if (isEnabledFor(selections)) {
			setEnabled(!getProvisioningUI().hasScheduledOperations());
		} else
			setEnabled(false);
	}

	protected abstract boolean isEnabledFor(Object[] selections);

	protected int getLock(IProfile profile, IInstallableUnit iu) {
		if (profile == null)
			return IProfile.LOCK_NONE;
		try {
			String value = profile.getInstallableUnitProperty(iu, IProfile.PROP_PROFILE_LOCKED_IU);
			if (value != null)
				return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			// ignore and assume no lock
		}
		return IProfile.LOCK_NONE;
	}

	protected String getProfileProperty(IProfile profile, IInstallableUnit iu, String propertyName) {
		if (profile == null || iu == null)
			return null;
		return profile.getInstallableUnitProperty(iu, propertyName);
	}

	protected void runCanceled() {
		// The action was canceled, do any cleanup needed before
		// it is run again.
		result = Window.CANCEL;
	}
}
