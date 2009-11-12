/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.operations;

import org.eclipse.equinox.p2.engine.IProvisioningPlan;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.operations.*;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

/**
 * @noextend
 * @since 2.0
 *
 */
public abstract class ProfileChangeOperation implements IProfileChangeJob {

	protected ProvisioningSession session;
	protected String profileId;
	protected ProvisioningContext context;
	protected String rootMarkerKey;
	MultiStatus noChangeRequest;
	PlannerResolutionJob job;
	ProfileChangeRequest request;

	public ProfileChangeOperation(ProvisioningSession session) {
		this.session = session;
		this.profileId = IProfileRegistry.SELF;
		this.rootMarkerKey = IProfile.PROP_PROFILE_ROOT_IU;
	}

	public IStatus resolveModal(IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		doResolve(monitor);
		if (job != null) {
			job.runModal(monitor);
		}
		return getResolutionResult();

	}

	public void setProfileId(String id) {
		this.profileId = id;
	}

	public void setRootMarkerKey(String propertyKey) {
		this.rootMarkerKey = propertyKey;
	}

	/**
	 * 
	 * @param monitor a progress monitor to use to report the job's progress.  This monitor will be called from a background thread.
	 * @return a job that can be scheduled to perform the provisioning operation.
	 */
	public ProvisioningJob getResolveJob(IProgressMonitor monitor) {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.ProfileChangeOperation_ResolveTaskName, 1000);
		doResolve(mon.newChild(100));
		job.setAdditionalProgressMonitor(mon.newChild(900));
		return job;
	}

	private void doResolve(IProgressMonitor monitor) {
		noChangeRequest = PlanAnalyzer.getProfileChangeAlteredStatus();
		if (session.hasScheduledOperationsFor(profileId)) {
			noChangeRequest.add(PlanAnalyzer.getStatus(IStatusCodes.OPERATION_ALREADY_IN_PROGRESS, null));
		} else {
			computeProfileChangeRequest(noChangeRequest, monitor);
		}
		if (request == null) {
			if (noChangeRequest.getChildren().length == 0)
				// No explanation for failure was provided.  It shouldn't happen, but...
				noChangeRequest = new MultiStatus(Activator.ID, IStatusCodes.UNEXPECTED_NOTHING_TO_DO, new IStatus[] {PlanAnalyzer.getStatus(IStatusCodes.UNEXPECTED_NOTHING_TO_DO, null)}, Messages.ProfileChangeOperation_NoProfileChangeRequest, null);
			return;
		}
		createPlannerResolutionJob();
	}

	protected abstract void computeProfileChangeRequest(MultiStatus status, IProgressMonitor monitor);

	void createPlannerResolutionJob() {
		job = new PlannerResolutionJob(getResolveJobName(), session, profileId, request, context, noChangeRequest);
	}

	protected abstract String getResolveJobName();

	protected abstract String getProvisioningJobName();

	/**
	 * Return a status indicating the result of resolving this
	 * operation.  A <code>null</code> return indicates that
	 * resolving has not occurred yet.
	 * 
	 * @return the status of the resolution, or <code>null</code>
	 * if it has not been resolved yet.
	 */
	public IStatus getResolutionResult() {
		if (job != null && job.getResolutionResult() != null)
			return job.getResolutionResult().getSummaryStatus();
		if (request == null && noChangeRequest != null) {
			// If there is only one child message, use the specific message
			if (noChangeRequest.getChildren().length == 1)
				return noChangeRequest.getChildren()[0];
			return noChangeRequest;
		}
		return null;
	}

	public String getResolutionDetails() {
		if (job != null && job.getResolutionResult() != null)
			return job.getResolutionResult().getSummaryReport();
		return null;

	}

	public String getResolutionDetails(IInstallableUnit iu) {
		if (job != null && job.getResolutionResult() != null)
			return job.getResolutionResult().getDetailedReport(new IInstallableUnit[] {iu});
		return null;

	}

	public IProvisioningPlan getProvisioningPlan() {
		if (job != null)
			return job.getProvisioningPlan();
		return null;
	}

	public ProfileChangeRequest getProfileChangeRequest() {
		if (job != null)
			return job.getProfileChangeRequest();
		return null;
	}

	public ProvisioningJob getProvisioningJob(IProgressMonitor monitor) {
		IStatus status = getResolutionResult();
		if (status.getSeverity() != IStatus.CANCEL && status.getSeverity() != IStatus.ERROR) {
			if (job.getProvisioningPlan() != null) {
				ProfileModificationJob pJob = new ProfileModificationJob(getProvisioningJobName(), session, profileId, job.getProvisioningPlan(), context);
				pJob.setAdditionalProgressMonitor(monitor);
				return pJob;
			}
		}
		return null;
	}

	public void setProvisioningContext(ProvisioningContext context) {
		this.context = context;
		if (job != null)
			job.setProvisioningContext(context);
	}

	public ProvisioningContext getProvisioningContext() {
		return context;
	}

	public String getProfileId() {
		return profileId;
	}

	public boolean hasResolved() {
		return getResolutionResult() != null;
	}

}
