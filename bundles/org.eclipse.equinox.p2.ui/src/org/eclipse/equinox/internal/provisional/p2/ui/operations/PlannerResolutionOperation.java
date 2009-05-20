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
package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.ui.IStatusCodes;
import org.eclipse.equinox.internal.provisional.p2.ui.ResolutionResult;

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
	ResolutionResult report;
	ProvisioningContext provisioningContext;

	public PlannerResolutionOperation(String label, String profileId, ProfileChangeRequest request, ProvisioningContext provisioningContext, MultiStatus additionalStatus, boolean isUser) {
		super(label);
		this.request = request;
		this.profileId = profileId;
		this.isUser = isUser;
		if (provisioningContext == null)
			this.provisioningContext = new ProvisioningContext();
		else
			this.provisioningContext = provisioningContext;
		Assert.isNotNull(additionalStatus);
		this.additionalStatus = additionalStatus;
	}

	public ProvisioningPlan getProvisioningPlan() {
		return plan;
	}

	public ProfileChangeRequest getProfileChangeRequest() {
		return request;
	}

	public ProvisioningContext getProvisioningContext() {
		return provisioningContext;
	}

	protected IStatus doExecute(IProgressMonitor monitor) throws ProvisionException {
		plan = ProvisioningUtil.getProvisioningPlan(request, provisioningContext, monitor);
		if (plan == null)
			return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, IStatusCodes.UNEXPECTED_NOTHING_TO_DO, ProvUIMessages.PlannerResolutionOperation_UnexpectedError, null);
		// We are reporting on our ability to get a plan, not on the status of the plan itself.
		// Callers will interpret and report the status as needed.
		return Status.OK_STATUS;
	}

	public ResolutionResult getResolutionResult() {
		if (report == null) {
			report = PlanAnalyzer.computeResolutionResult(request, plan, additionalStatus);
		}
		return report;
	}

	public boolean runInBackground() {
		return true;
	}

	public boolean isUser() {
		return isUser;
	}
}
