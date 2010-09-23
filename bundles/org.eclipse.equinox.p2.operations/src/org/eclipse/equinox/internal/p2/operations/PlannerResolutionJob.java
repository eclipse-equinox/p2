/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.planner.IPlanner;

/**
 * Class representing a provisioning profile plan
 *
 * @since 2.0
 */
public class PlannerResolutionJob extends ProvisioningJob implements IProfileChangeJob {

	ProfileChangeRequest request;
	String profileId;
	IProvisioningPlan plan;
	MultiStatus additionalStatus;
	ResolutionResult report;
	ProvisioningContext firstPass, successful;
	IFailedStatusEvaluator evaluator;

	public static MultiStatus getProfileChangeRequestAlteredStatus() {
		return PlanAnalyzer.getProfileChangeAlteredStatus();
	}

	public PlannerResolutionJob(String label, ProvisioningSession session, String profileId, ProfileChangeRequest request, ProvisioningContext context, IFailedStatusEvaluator evaluator, MultiStatus additionalStatus) {
		super(label, session);
		this.request = request;
		this.profileId = profileId;
		if (context == null)
			firstPass = new ProvisioningContext(session.getProvisioningAgent());
		else
			firstPass = context;
		this.evaluator = evaluator;
		Assert.isNotNull(additionalStatus);
		this.additionalStatus = additionalStatus;
	}

	public IProvisioningPlan getProvisioningPlan() {
		return plan;
	}

	public ProfileChangeRequest getProfileChangeRequest() {
		return request;
	}

	public ProvisioningContext getActualProvisioningContext() {
		return successful;
	}

	public void setFirstPassProvisioningContext(ProvisioningContext firstPass) {
		this.firstPass = firstPass;
	}

	public IStatus runModal(IProgressMonitor monitor) {
		SubMonitor sub;
		if (evaluator != null) {
			sub = SubMonitor.convert(monitor, 1000);
		} else {
			sub = SubMonitor.convert(monitor, 500);
		}

		plan = ((IPlanner) getSession().getProvisioningAgent().getService(IPlanner.SERVICE_NAME)).getProvisioningPlan(request, firstPass, sub.newChild(500));
		IStatus status;
		if (plan == null) {
			status = new Status(IStatus.ERROR, Activator.ID, Messages.PlannerResolutionJob_NullProvisioningPlan);
			additionalStatus.add(status);
		} else {
			status = plan.getStatus();
		}

		if (status.getSeverity() != IStatus.ERROR || evaluator == null) {
			successful = firstPass;
			return status;
		}

		// First resolution was in error, try again with an alternate provisioning context
		ProvisioningContext secondPass = evaluator.getSecondPassProvisioningContext(plan);
		if (secondPass == null)
			return status;

		successful = secondPass;
		plan = ((IPlanner) getSession().getProvisioningAgent().getService(IPlanner.SERVICE_NAME)).getProvisioningPlan(request, secondPass, sub.newChild(500));
		if (plan == null) {
			status = new Status(IStatus.ERROR, Activator.ID, Messages.PlannerResolutionJob_NullProvisioningPlan);
			additionalStatus.add(status);
			return status;
		}
		return plan.getStatus();
	}

	public ResolutionResult getResolutionResult() {
		if (report == null) {
			if (plan == null) {
				if (additionalStatus.getSeverity() != IStatus.ERROR) {
					additionalStatus.add(new Status(IStatus.ERROR, Activator.ID, Messages.PlannerResolutionJob_NullProvisioningPlan));
				}
				report = new ResolutionResult();
				report.addSummaryStatus(additionalStatus);
			} else {
				report = PlanAnalyzer.computeResolutionResult(request, plan, additionalStatus);
			}
		}
		return report;
	}

	public String getProfileId() {
		return profileId;
	}
}
