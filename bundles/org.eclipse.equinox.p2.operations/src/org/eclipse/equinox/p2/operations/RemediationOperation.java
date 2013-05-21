/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.operations;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.operations.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;

/**
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same. Please do not use this API without
 * consulting with the p2 team.
 * </p>
 * @noreference
 * @since 2.3
 */
public class RemediationOperation extends ProfileChangeOperation {

	private static int ZERO_WEIGHT = 0;
	private static int LOW_WEIGHT = 1;
	private static int MEDIUM_WEIGHT = 2;
	private static int HIGH_WEIGHT = 3;
	private List<Remedy> remedies;
	private Remedy bestSolutionChangingTheRequest;
	private Remedy bestSolutionChangingWhatIsInstalled;
	private Remedy currentRemedy;
	private RemedyConfig[] remedyConfigs;

	public RemedyConfig[] getRemedyConfigs() {
		return remedyConfigs;
	}

	public Remedy getCurrentRemedy() {
		return currentRemedy;
	}

	public void setCurrentRemedy(Remedy currentRemedy) {
		this.currentRemedy = currentRemedy;
		request = currentRemedy == null ? null : currentRemedy.getRequest();
	}

	private IProfileChangeRequest originalRequest;

	public RemediationOperation(ProvisioningSession session, IProfileChangeRequest iProfileChangeRequest) {
		this(session, iProfileChangeRequest, RemedyConfig.getAllRemedyConfigs());

	}

	public RemediationOperation(ProvisioningSession session, IProfileChangeRequest originalRequest, RemedyConfig[] remedyConfigs) {
		super(session);
		this.originalRequest = originalRequest;
		remedies = new ArrayList<Remedy>();
		this.remedyConfigs = remedyConfigs;
	}

	public Remedy bestSolutionChangingTheRequest() {
		return bestSolutionChangingTheRequest;
	}

	public Remedy bestSolutionChangingWhatIsInstalled() {
		return bestSolutionChangingWhatIsInstalled;
	}

	public List<Remedy> getRemedies() {
		return remedies;
	}

	@Override
	protected void computeProfileChangeRequest(MultiStatus status, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, 1);
		if (currentRemedy != null) {
			request = currentRemedy.getRequest();
			sub.worked(1);
			return;
		}
		try {
			status.add(computeAllRemediations(sub.newChild(1)));
		} catch (OperationCanceledException e) {
			status.add(Status.CANCEL_STATUS);
		}
		determineBestSolutions();
	}

	private IStatus computeAllRemediations(IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, remedyConfigs.length);
		sub.setTaskName(Messages.RemediationOperation_ProfileChangeRequestProgress);
		List<Remedy> tmpRemedies = new ArrayList<Remedy>(remedyConfigs.length);
		try {
			for (int i = 0; i < remedyConfigs.length; i++) {
				sub.subTask((i + 1) + " / " + remedyConfigs.length); //$NON-NLS-1$
				if (sub.isCanceled())
					return Status.CANCEL_STATUS;
				Remedy remedy = computeRemedy(remedyConfigs[i], sub.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
				if (remedy != null) {
					tmpRemedies.add(remedy);
				}
			}
		} finally {
			sub.done();
		}
		remedies = tmpRemedies;
		return getResolutionResult();
	}

	private void determineBestSolutions() {
		int beingInstalledWeight = 0;
		int installationWeight = 0;
		for (Iterator<Remedy> iterator = remedies.iterator(); iterator.hasNext();) {
			Remedy remedy = iterator.next();
			if (remedy.getRequest() != null) {
				if (remedy.getBeingInstalledRelaxedWeight() > beingInstalledWeight && remedy.getInstallationRelaxedWeight() == 0) {
					bestSolutionChangingTheRequest = remedy;
					beingInstalledWeight = remedy.getBeingInstalledRelaxedWeight();
					continue;
				}
				if (remedy.getInstallationRelaxedWeight() > installationWeight && remedy.getBeingInstalledRelaxedWeight() == 0) {
					bestSolutionChangingWhatIsInstalled = remedy;
					installationWeight = remedy.getInstallationRelaxedWeight();
					continue;
				}
			}
		}
	}

	private Remedy computeRemedy(RemedyConfig configuration, IProgressMonitor monitor) {
		Remedy remedy = new Remedy(originalRequest);
		remedy.setConfig(configuration);
		IPlanner planner = session.getPlanner();
		RequestFlexer av = new RequestFlexer(planner);
		av.setAllowDifferentVersion(configuration.allowDifferentVersion);
		av.setAllowInstalledElementChange(configuration.allowInstalledUpdate);
		av.setAllowInstalledElementRemoval(configuration.allowInstalledRemoval);
		av.setAllowPartialInstall(configuration.allowPartialInstall);
		av.setProvisioningContext(getProvisioningContext());
		remedy.setRequest((ProfileChangeRequest) av.getChangeRequest(originalRequest, ((ProfileChangeRequest) originalRequest).getProfile(), monitor));
		if (remedy.getRequest() == null)
			return null;

		if (configuration.allowInstalledUpdate && !configuration.allowInstalledRemoval) {
			remedy.setInstallationRelaxedWeight(HIGH_WEIGHT);
		} else if (!configuration.allowInstalledUpdate && configuration.allowInstalledRemoval) {
			remedy.setInstallationRelaxedWeight(MEDIUM_WEIGHT);
		} else if (configuration.allowInstalledUpdate && configuration.allowInstalledRemoval) {
			remedy.setInstallationRelaxedWeight(LOW_WEIGHT);
		} else
			remedy.setInstallationRelaxedWeight(ZERO_WEIGHT);

		if (configuration.allowDifferentVersion && !configuration.allowPartialInstall) {
			remedy.setBeingInstalledRelaxedWeight(HIGH_WEIGHT);
		} else if (!configuration.allowDifferentVersion && configuration.allowPartialInstall) {
			remedy.setBeingInstalledRelaxedWeight(MEDIUM_WEIGHT);
		} else if (configuration.allowDifferentVersion && configuration.allowPartialInstall) {
			remedy.setBeingInstalledRelaxedWeight(LOW_WEIGHT);
		} else {
			remedy.setBeingInstalledRelaxedWeight(ZERO_WEIGHT);
		}
		computeRemedyDetails(remedy);
		return remedy;
	}

	@Override
	protected String getResolveJobName() {
		return Messages.RemediationOperation_ResolveJobName;
	}

	@Override
	protected String getProvisioningJobName() {
		return Messages.RemediationOperation_RemediationJobName;
	}

	public ProvisioningJob getProvisioningJob(IProgressMonitor monitor) {
		IStatus status = getResolutionResult();
		if (status.getSeverity() != IStatus.CANCEL && status.getSeverity() != IStatus.ERROR) {
			if (job.getProvisioningPlan() != null) {
				ProfileModificationJob pJob = new ProfileModificationJob(getProvisioningJobName(), session, profileId, job.getProvisioningPlan(), job.getActualProvisioningContext());
				pJob.setAdditionalProgressMonitor(monitor);
				return pJob;
			}
		}
		return null;
	}

	public ProfileChangeRequest getOriginalRequest() {
		return (ProfileChangeRequest) originalRequest;
	}

	void makeResolveJob(final IProgressMonitor monitor) {
		// throw away any previous requests
		request = null;
		noChangeRequest = PlanAnalyzer.getProfileChangeAlteredStatus();
		// the requestHolder is a hack to work around the fact that there is no public API
		// for the resolution job to get the request from the operation after it has been
		// computed.
		final ProfileChangeRequest[] requestHolder = new ProfileChangeRequest[1];
		job = new RemediationResolutionJob(getResolveJobName(), session, profileId, request, getFirstPassProvisioningContext(), getSecondPassEvaluator(), noChangeRequest, new IRunnableWithProgress() {
			public void run(IProgressMonitor mon) throws OperationCanceledException {
				SubMonitor sub = SubMonitor.convert(mon, 2);
				// We only check for other jobs running if this job is *not* scheduled
				if (job.getState() == Job.NONE && session.hasScheduledOperationsFor(profileId)) {
					noChangeRequest.add(PlanAnalyzer.getStatus(IStatusCodes.OPERATION_ALREADY_IN_PROGRESS, null));
				} else {
					sub.worked(1);
					computeProfileChangeRequest(noChangeRequest, sub.newChild(1));
					requestHolder[0] = RemediationOperation.this.request;
				}
			}
		}, requestHolder, this);
	}

	@Override
	public IStatus getResolutionResult() {
		if (currentRemedy != null)
			return super.getResolutionResult();
		return remedies.size() > 0 ? Status.OK_STATUS : new Status(IStatus.ERROR, Activator.ID, Messages.RemediationOperation_NoRemedyFound);
	}

	private void computeRemedyDetails(Remedy remedy) {
		ArrayList<String> updateIds = new ArrayList<String>();
		for (IInstallableUnit addedIU : remedy.getRequest().getAdditions()) {
			for (IInstallableUnit removedIU : remedy.getRequest().getRemovals()) {
				if (removedIU.getId().equals(addedIU.getId())) {
					createModificationRemedyDetail(addedIU, removedIU, remedy);
					updateIds.add(addedIU.getId());
					break;
				}
			}
			if (!updateIds.contains(addedIU.getId())) {
				createAdditionRemedyDetail(addedIU, remedy);
			}
		}

		for (IInstallableUnit removedIU : remedy.getRequest().getRemovals()) {
			if (!updateIds.contains(removedIU.getId())) {
				createRemovalRemedyDetail(removedIU, remedy);
			}
		}

		for (IInstallableUnit addedIUinOriginalRequest : originalRequest.getAdditions()) {
			boolean found = false;
			for (IInstallableUnit addedIU : remedy.getRequest().getAdditions()) {
				if (addedIU.getId().equals(addedIUinOriginalRequest.getId())) {
					found = true;
					break;
				}
			}
			if (!found) {
				createNotAddedRemedyDetail(addedIUinOriginalRequest, remedy);
				found = false;
			}
		}
	}

	private void createNotAddedRemedyDetail(IInstallableUnit iu, Remedy remedy) {
		RemedyIUDetail iuDetail = new RemedyIUDetail(iu);
		iuDetail.setStatus(RemedyIUDetail.STATUS_NOT_ADDED);
		iuDetail.setRequestedVersion(iu.getVersion());
		remedy.addRemedyIUDetail(iuDetail);
	}

	private void createRemovalRemedyDetail(IInstallableUnit iu, Remedy remedy) {
		RemedyIUDetail iuDetail = new RemedyIUDetail(iu);
		iuDetail.setStatus(RemedyIUDetail.STATUS_REMOVED);
		iuDetail.setInstalledVersion(iu.getVersion());
		remedy.addRemedyIUDetail(iuDetail);
	}

	private void createAdditionRemedyDetail(IInstallableUnit iu, Remedy remedy) {
		RemedyIUDetail iuDetail = new RemedyIUDetail(iu);
		iuDetail.setStatus(RemedyIUDetail.STATUS_ADDED);
		iuDetail.setBeingInstalledVersion(iu.getVersion());
		iuDetail.setRequestedVersion(searchInOriginalRequest(iu.getId()));
		remedy.addRemedyIUDetail(iuDetail);
	}

	private void createModificationRemedyDetail(IInstallableUnit beingInstalledIU, IInstallableUnit installedIU, Remedy remedy) {
		RemedyIUDetail iuDetail = new RemedyIUDetail(beingInstalledIU);
		iuDetail.setStatus(RemedyIUDetail.STATUS_CHANGED);
		iuDetail.setBeingInstalledVersion(beingInstalledIU.getVersion());
		iuDetail.setInstalledVersion(installedIU.getVersion());
		iuDetail.setRequestedVersion(searchInOriginalRequest(beingInstalledIU.getId()));
		remedy.addRemedyIUDetail(iuDetail);
	}

	private Version searchInOriginalRequest(String id) {
		for (IInstallableUnit iu : originalRequest.getAdditions()) {
			if (iu.getId() == id)
				return iu.getVersion();
		}
		return null;
	}
}
