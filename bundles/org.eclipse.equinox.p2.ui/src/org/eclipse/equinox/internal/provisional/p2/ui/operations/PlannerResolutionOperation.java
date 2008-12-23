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
package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.IStatusCodes;

/**
 * Class representing a provisioning profile plan
 *
 * @since 3.4
 */
public class PlannerResolutionOperation extends ProvisioningOperation {

	ProfileChangeRequest request;
	String profileId;
	boolean isUser = true;
	ProvisioningPlan plan;
	MultiStatus additionalStatus;
	IInstallableUnit[] iusInvolved;

	public PlannerResolutionOperation(String label, IInstallableUnit[] iusInvolved, String profileId, ProfileChangeRequest request, MultiStatus additionalStatus, boolean isUser) {
		super(label);
		this.request = request;
		this.profileId = profileId;
		this.isUser = isUser;
		this.iusInvolved = iusInvolved;
		this.additionalStatus = additionalStatus;
	}

	public ProvisioningPlan getProvisioningPlan() {
		return plan;
	}

	protected IStatus doExecute(IProgressMonitor monitor) throws ProvisionException {
		plan = ProvisioningUtil.getProvisioningPlan(request, new ProvisioningContext(), monitor);
		if (plan == null)
			return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, IStatusCodes.UNEXPECTED_NOTHING_TO_DO, ProvUIMessages.PlannerResolutionOperation_UnexpectedError, null);
		// If we recorded additional status along the way, build a plan that merges in this status.
		// Ideally this all would have been detected in the planner itself.
		if (additionalStatus.getChildren().length > 0) {
			additionalStatus.merge(plan.getStatus());
			plan = new ProvisioningPlan(additionalStatus, plan.getOperands(), null);
		}
		// Now run the result through the sanity checker.  Again, this would ideally be caught
		// in the planner, but for now we have to build a new plan to include the UI status checking.
		plan = new ProvisioningPlan(PlanStatusHelper.computeStatus(plan, iusInvolved), plan.getOperands(), null);

		// We are reporting on our ability to get a plan, not on the status of the plan itself.
		// Callers will interpret and report the status as needed.
		return Status.OK_STATUS;
	}

	public boolean runInBackground() {
		return true;
	}

	public boolean isUser() {
		return isUser;
	}
}
