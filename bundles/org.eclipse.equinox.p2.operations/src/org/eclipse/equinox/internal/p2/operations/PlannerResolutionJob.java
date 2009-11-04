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
package org.eclipse.equinox.internal.p2.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;

/**
 * Class representing a provisioning profile plan
 *
 * @since 2.0
 */
public class PlannerResolutionJob extends ProvisioningJob {

	ProfileChangeRequest request;
	String profileId;
	ProvisioningPlan plan;
	MultiStatus additionalStatus;
	ResolutionResult report;
	ProvisioningContext provisioningContext;

	public static MultiStatus getProfileChangeRequestAlteredStatus() {
		return PlanAnalyzer.getProfileChangeAlteredStatus();
	}

	public PlannerResolutionJob(String label, ProvisioningSession session, String profileId, ProfileChangeRequest request, ProvisioningContext provisioningContext, MultiStatus additionalStatus) {
		super(label, session);
		this.request = request;
		this.profileId = profileId;
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

	public void setProvisioningContext(ProvisioningContext context) {
		this.provisioningContext = context;
	}

	public void runModal(IProgressMonitor monitor) throws ProvisionException {
		plan = getSession().getProvisioningPlan(request, provisioningContext, monitor);

	}

	public ResolutionResult getResolutionResult() {
		if (report == null) {
			report = PlanAnalyzer.computeResolutionResult(request, plan, additionalStatus);
		}
		return report;
	}
}
