/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.UpdateAction;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.PlanValidator;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Overridden to hide the resolution from the user and optionally suppress 
 * the wizard if we are resolving for reasons other than user request.
 * 
 * @since 3.5
 * 
 */
final class AutomaticUpdateAction extends UpdateAction {

	/**
	 * 
	 */
	final AutomaticUpdater automaticUpdater;
	private boolean suppressWizard = false;
	private PlannerResolutionOperation resolvedOperation;
	private ProvUIProvisioningListener profileListener;

	AutomaticUpdateAction(AutomaticUpdater automaticUpdater,
			ISelectionProvider selectionProvider, String profileId,
			PlannerResolutionOperation op, IUElementListRoot root,
			ArrayList initialSelections) {
		super(new Policy(), selectionProvider, profileId, false);
		ProvUI.addProvisioningListener(createProfileListener());
		this.resolvedOperation = op;
		this.automaticUpdater = automaticUpdater;
		this.root = root;
		this.initialSelections = initialSelections;
	}

	private ProvUIProvisioningListener createProfileListener() {
		profileListener = new ProvUIProvisioningListener(
				ProvUIProvisioningListener.PROV_EVENT_PROFILE) {
			protected void profileChanged(final String profileId) {
				String id = getProfileId(false);
				if (id == IProfileRegistry.SELF) {
					try {
						IProfile profile = ProvisioningUtil.getProfile(IProfileRegistry.SELF);
						id = profile.getProfileId();
					} catch (ProvisionException e) {
						id = null;
					}
				}
				if (profileId.equals(id)) {
					resolvedOperation = null;
					automaticUpdater.validateUpdates();
				}
			}
		};
		return profileListener;
	}

	protected void run(final IInstallableUnit[] ius, final String id) {
		// Do we have a plan??
		if (resolvedOperation != null
				&& resolvedOperation.getProvisioningPlan() != null) {
			if (PlatformUI.isWorkbenchRunning()) {
				PlatformUI.getWorkbench().getDisplay().asyncExec(
						new Runnable() {
							public void run() {
								if (validatePlan(resolvedOperation
										.getProvisioningPlan())) {
									performAction(ius, id, resolvedOperation);
								}
							}
						});
			}
		} else
			super.run(ius, id);
	}

	void suppressWizard(boolean suppress) {
		suppressWizard = suppress;
	}

	protected int performAction(IInstallableUnit[] ius, String targetProfileId,
			PlannerResolutionOperation resolution) {
		if (suppressWizard) {
			automaticUpdater.setUpdateAffordanceState(resolution != null
					&& resolution.getResolutionResult().getSummaryStatus()
							.isOK());
			return Window.OK;
		}
		return super.performAction(ius, targetProfileId, resolution);
	}

	protected PlanValidator getPlanValidator() {
		return new PlanValidator() {
			public boolean continueWorkingWithPlan(ProvisioningPlan plan,
					Shell shell) {
				if (automaticUpdater.alreadyValidated)
					return true;
				// In all other cases we return false, because clicking the
				// popup will actually run the action.
				// We are just determining whether to show the popup or not.
				if (plan != null) {
					// If the user cancelled the operation, don't continue
					if (plan.getStatus().getSeverity() == IStatus.CANCEL)
						return false;
					boolean noError = plan.getStatus().getSeverity() != IStatus.ERROR;
					// Show the affordance regardless of the status since
					// updates were found.
					if (automaticUpdater.updateAffordance == null)
						automaticUpdater.createUpdateAffordance();
					automaticUpdater.setUpdateAffordanceState(noError);
					if (noError && automaticUpdater.popup == null)
						automaticUpdater.createUpdatePopup();
				}
				return false;
			}
		};
	}

	public void dispose() {
		if (profileListener != null)
			ProvUI.removeProvisioningListener(profileListener);
		profileListener = null;
	}
}