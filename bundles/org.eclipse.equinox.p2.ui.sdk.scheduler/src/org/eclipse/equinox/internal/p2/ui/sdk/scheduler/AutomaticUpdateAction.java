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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.UpdateAction;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.PlanValidator;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

/**
 * Overridden so that we can use the profile change request computations,
 * but we hide the resolution from the user and optionally suppress the
 * wizard if we are resolving for reasons other than user request.
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

	AutomaticUpdateAction(AutomaticUpdater automaticUpdater, ISelectionProvider selectionProvider, String profileId) {
		super(new Policy(), selectionProvider, profileId, false);
		this.automaticUpdater = automaticUpdater;
	}

	void suppressWizard(boolean suppress) {
		suppressWizard = suppress;
	}

	protected int performAction(IInstallableUnit[] ius, String targetProfileId, ProvisioningPlan plan) {
		if (suppressWizard) {
			automaticUpdater.setUpdateAffordanceState(plan != null && plan.getStatus().isOK());
			return Window.OK;
		}
		return super.performAction(ius, targetProfileId, plan);
	}

	protected PlanValidator getPlanValidator() {
		return new PlanValidator() {
			public boolean continueWorkingWithPlan(ProvisioningPlan plan, Shell shell) {
				if (automaticUpdater.alreadyValidated)
					return true;
				// In all other cases we return false, because clicking the popup will actually run the action.
				// We are just determining whether to show the popup or not.
				if (plan != null) {
					// If the user cancelled the operation, don't continue
					if (plan.getStatus().getSeverity() == IStatus.CANCEL)
						return false;
					boolean noError = plan.getStatus().getSeverity() != IStatus.ERROR;
					// Show the affordance regardless of the status since updates were found.
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
}