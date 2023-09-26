/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.operations.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;

/**
 * ProfileChangeOperation describes a provisioning operation that modifies a profile.
 * The operation must first be resolved, followed by the actual provisioning
 * work being performed.  This two-pass nature of the ProfileChangeOperation allows
 * resolution status to be reported to a client to determine whether the operation
 * should proceed.  Each phase of the operation can be performed synchronously or in
 * the background as a job.  To perform the operation synchronously:
 *
 *  <pre>
 *     IStatus result = op.resolveModal(monitor);
 *     if (result.isOK())
 *       op.getProvisioningJob(null).runModal(monitor);
 *     else {
 *       // interpret the result
 *     }
 *  </pre>
 *
 *  To perform the resolution synchronously and the provisioning job in the
 *  background:
 *
 *  <pre>
 *     IStatus status = op.resolveModal(monitor);
 *     if (status.isOK()) {
 *       ProvisioningJob job = op.getProvisioningJob(monitor);
 *       job.schedule();
 *     } else {
 *       // interpret the result
 *     }
 *  </pre>
 *
 *  To resolve in the background and perform the job when it is complete:
 *
 *  <pre>
 *     ProvisioningJob job = op.getResolveJob(monitor);
 *     job.addJobChangeListener(new JobChangeAdapter() {
 *       public void done (JobChangeEvent event) {
 *         if (event.getResult().isOK() {
 *            op.getProvisioningJob(monitor).schedule();
 *         } else {
 *            // interpret the result
 *         }
 *       }
 *     });
 *     job.schedule();
 *
 *  </pre>
 *
 * In general, it is expected that clients create a new ProfileChangeOperation if
 * the resolution result of the current operation is not satisfactory.  However,
 * subclasses may prescribe a different life cycle where appropriate.
 *
 * When retrieving the resolution and provisioning jobs managed by this operation,
 * a client may supply a progress monitor to be used by the job.  When the job is
 * run by the platform job manager, both the platform job manager progress indicator
 * and the monitor supplied by the client will be updated.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public abstract class ProfileChangeOperation implements IProfileChangeJob {

	ProvisioningSession session;
	String profileId;
	ProvisioningContext context;
	MultiStatus noChangeRequest;
	PlannerResolutionJob job;
	ProfileChangeRequest request;

	/**
	 * Create an operation using the provided provisioning session.
	 * Unless otherwise specified by the client, the operation is
	 * performed on the currently running profile.
	 *
	 * @param session the provisioning session providing the services
	 */
	protected ProfileChangeOperation(ProvisioningSession session) {
		this.session = session;
		this.profileId = IProfileRegistry.SELF;
		this.context = new ProvisioningContext(session.getProvisioningAgent());
	}

	/**
	 * Resolve the operation in the current thread using the specified progress
	 * monitor.  Return a status describing the result of the resolution.
	 *
	 * @param monitor the progress monitor to use
	 * @return a status describing the resolution results
	 */
	public final IStatus resolveModal(IProgressMonitor monitor) {
		SubMonitor subMon = SubMonitor.convert(monitor, 2);
		prepareToResolve();
		makeResolveJob(subMon.split(1));
		if (job != null) {
			IStatus status = job.runModal(subMon.split(1));
			if (status.getSeverity() == IStatus.CANCEL)
				return Status.CANCEL_STATUS;
		}
		monitor.done();
		// For anything other than cancellation, we examine the artifacts of the resolution and come
		// up with an overall summary.
		return getResolutionResult();

	}

	/**
	 * Set the id of the profile that will be modified by this operation.
	 * @param id the profile id
	 */
	public void setProfileId(String id) {
		this.profileId = id;
	}

	/**
	 * Return a job that can be used to resolve this operation in the background.
	 *
	 * @param monitor a progress monitor that should be used to report the job's progress in addition
	 * to the standard job progress reporting.  Can be <code>null</code>.  If provided, this monitor
	 * will be called from a background thread.
	 *
	 * @return a job that can be scheduled to perform the provisioning operation.
	 */
	public final ProvisioningJob getResolveJob(IProgressMonitor monitor) {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.ProfileChangeOperation_ResolveTaskName, 1000);
		prepareToResolve();
		makeResolveJob(mon.newChild(100));
		if (mon.isCanceled())
			return null;
		if (job != null)
			job.setAdditionalProgressMonitor(mon.newChild(900));
		return job;
	}

	/**
	 * Perform any processing that must occur just before resolving this operation.
	 */
	protected void prepareToResolve() {
		// default is to do nothing
	}

	void makeResolveJob(IProgressMonitor monitor) {
		noChangeRequest = PlanAnalyzer.getProfileChangeAlteredStatus();
		if (session.hasScheduledOperationsFor(profileId)) {
			noChangeRequest.add(PlanAnalyzer.getStatus(IStatusCodes.OPERATION_ALREADY_IN_PROGRESS, null));
		} else {
			computeProfileChangeRequest(noChangeRequest, monitor);
		}
		if (request == null) {
			if (noChangeRequest.getChildren().length == 0)
				// No explanation for failure was provided.  It shouldn't happen, but...
				noChangeRequest = new MultiStatus(Constants.BUNDLE_ID, IStatusCodes.UNEXPECTED_NOTHING_TO_DO, new IStatus[] {PlanAnalyzer.getStatus(IStatusCodes.UNEXPECTED_NOTHING_TO_DO, null)}, Messages.ProfileChangeOperation_NoProfileChangeRequest, null);
			return;
		}
		createPlannerResolutionJob();
	}

	/**
	 * Compute the profile change request for this operation, adding any relevant intermediate status
	 * to the supplied status.
	 *
	 * @param status a multi-status to be used to add relevant status.  If a profile change request cannot
	 * be computed for any reason, a status should be added to explain the problem.
	 *
	 * @param monitor the progress monitor to use for computing the profile change request
	 */
	protected abstract void computeProfileChangeRequest(MultiStatus status, IProgressMonitor monitor);

	private void createPlannerResolutionJob() {
		job = new PlannerResolutionJob(getResolveJobName(), session, profileId, request, getFirstPassProvisioningContext(), getSecondPassEvaluator(), noChangeRequest);
	}

	/**
	 * Return an appropriate name for the resolution job.
	 *
	 * @return the resolution job name.
	 */
	protected abstract String getResolveJobName();

	/**
	 * Return an appropriate name for the provisioning job.
	 *
	 * @return the provisioning job name.
	 */
	protected abstract String getProvisioningJobName();

	/**
	 * Return a status indicating the result of resolving this
	 * operation.  A <code>null</code> return indicates that
	 * resolving has not occurred yet.
	 *
	 * @return the status of the resolution, or <code>null</code>
	 * if resolution has not yet occurred.
	 */
	public IStatus getResolutionResult() {
		if (request == null) {
			if (noChangeRequest != null) {
				// If there is only one child message, use the specific message
				if (noChangeRequest.getChildren().length == 1)
					return noChangeRequest.getChildren()[0];
				return noChangeRequest;
			}
			return null;
		}
		if (job != null && job.getResolutionResult() != null)
			return job.getResolutionResult().getSummaryStatus();
		return null;
	}

	/**
	 * Return a string that can be used to describe the results of the resolution
	 * to a client.
	 *
	 * @return a string describing the resolution details, or <code>null</code> if the
	 * operation has not been resolved.
	 */
	public String getResolutionDetails() {
		if (job != null && job.getResolutionResult() != null)
			return job.getResolutionResult().getSummaryReport();
		// We couldn't resolve, but we have some status describing
		// why there is no profile change request.
		IStatus result = getResolutionResult();
		if (result != null)
			return result.getMessage();
		return null;

	}

	/**
	 * Return a string that describes the specific resolution results
	 * related to the supplied {@link IInstallableUnit}.
	 *
	 * @param iu the IInstallableUnit for which resolution details are requested
	 *
	 * @return a string describing the results for the installable unit, or <code>null</code> if
	 * there are no specific results available for the installable unit.
	 */
	public String getResolutionDetails(IInstallableUnit iu) {
		if (job != null && job.getResolutionResult() != null)
			return job.getResolutionResult().getDetailedReport(new IInstallableUnit[] {iu});
		return null;

	}

	/**
	 * Return the provisioning plan obtained by resolving the receiver.
	 *
	 * @return the provisioning plan.  This may be <code>null</code> if the operation
	 * has not been resolved, or if a plan could not be obtained when attempting to
	 * resolve.  If the plan is null and the operation has been resolved, then the
	 * resolution result will explain the problem.
	 *
	 * @see #hasResolved()
	 * @see #getResolutionResult()
	 */
	public IProvisioningPlan getProvisioningPlan() {
		if (job != null)
			return job.getProvisioningPlan();
		return null;
	}

	/**
	 * Return the profile change request that describes the receiver.
	 *
	 * @return the profile change request.  This may be <code>null</code> if the operation
	 * has not been resolved, or if a profile change request could not be assembled given
	 * the operation's state.  If the profile change request is null and the operation has
	 * been resolved, the the resolution result will explain the problem.
	 *
	 * @see #hasResolved()
	 * @see #getResolutionResult()
	 * @since 2.1
	 */
	public IProfileChangeRequest getProfileChangeRequest() {
		if (job != null)
			return job.getProfileChangeRequest();
		return null;
	}

	/**
	 * Return a provisioning job that can be used to perform the resolved operation.  The job is
	 * created using the default values associated with a new job.  It is up to clients to configure
	 * the priority of the job and set any appropriate properties, such as
	 * {@link Job#setUser(boolean)},
	 * {@link Job#setSystem(boolean)}, or {@link Job#setProperty(QualifiedName, Object)},
	 * before scheduling it.
	 *
	 * @param monitor a progress monitor that should be used to report the job's progress in addition
	 * to the standard job progress reporting.  Can be <code>null</code>.  If provided, this monitor
	 * will be called from a background thread.
	 *
	 * @return a job that can be used to perform the provisioning operation.  This may be <code>null</code>
	 * if the operation has not been resolved, or if a plan could not be obtained when attempting to
	 * resolve.  If the job is null and the operation has been resolved, then the resolution result
	 * will explain the problem.
	 *
	 * @see #hasResolved()
	 * @see #getResolutionResult()
	 */
	public ProvisioningJob getProvisioningJob(IProgressMonitor monitor) {
		IStatus status = getResolutionResult();
		//if status is null we haven't resolved yet, so we must return null here
		if (status == null)
			return null;
		if (status.getSeverity() != IStatus.CANCEL && status.getSeverity() != IStatus.ERROR) {
			if (job.getProvisioningPlan() != null) {
				ProfileModificationJob pJob = new ProfileModificationJob(getProvisioningJobName(), session, profileId, job.getProvisioningPlan(), job.getActualProvisioningContext());
				pJob.setAdditionalProgressMonitor(monitor);
				return pJob;
			}
		}
		return null;
	}

	/**
	 * Set the provisioning context that should be used to resolve and perform the provisioning for
	 * the operation.  This must be set before an attempt is made to resolve the operation
	 * for it to have any effect.
	 *
	 * @param context the provisioning context.
	 */
	public void setProvisioningContext(ProvisioningContext context) {
		this.context = context;
		if (job != null)
			updateJobProvisioningContexts(job, context);
	}

	/**
	 * Get the provisioning context that will be used to resolve and perform the provisioning for
	 * the operation.
	 *
	 * @return the provisioning context
	 */
	public ProvisioningContext getProvisioningContext() {
		return context;
	}

	@Override
	public String getProfileId() {
		return profileId;
	}

	/**
	 * Return a boolean indicating whether the operation has been resolved.  This method
	 * should be used to determine whether a client can expect to retrieve a profile
	 * change request, provisioning plan, or resolution result.  It is possible that this
	 * method return <code>false</code> while resolution is taking place if it is performed
	 * in the background.
	 *
	 * @return <code>true</code> if the operation has been resolved, <code>false</code>
	 * if it has not resolved.
	 */
	public boolean hasResolved() {
		return getResolutionResult() != null;
	}

	ProvisioningContext getFirstPassProvisioningContext() {
		return getProvisioningContext();
	}

	IFailedStatusEvaluator getSecondPassEvaluator() {
		return failedPlan -> null;
	}

	protected void updateJobProvisioningContexts(PlannerResolutionJob resolutionJob, ProvisioningContext provisioningContext) {
		resolutionJob.setFirstPassProvisioningContext(provisioningContext);
	}

}
