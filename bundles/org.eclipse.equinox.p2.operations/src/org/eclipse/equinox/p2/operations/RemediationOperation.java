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
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.internal.p2.operations.RequestFlexer;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;

//TODO Javadoc
public class RemediationOperation extends ProfileChangeOperation {

	private static int ZERO_WEIGHT = 0;
	private static int LOW_WEIGHT = 1;
	private static int MEDIUM_WEIGHT = 2;
	private static int HIGH_WEIGHT = 3;
	private List<Remedy> remedies;
	private Remedy bestSolutionChangingTheRequest;
	private Remedy bestSolutionChangingWhatIsInstalled;
	private Remedy currentRemedy;

	public Remedy getCurrentRemedy() {
		return currentRemedy;
	}

	public void setCurrentRemedy(Remedy currentRemedy) {
		this.currentRemedy = currentRemedy;
	}

	private IProfileChangeRequest originalRequest;
	private boolean isCheckForUpdates;

	public boolean isCheckForUpdates() {
		return isCheckForUpdates;
	}

	public RemediationOperation(ProvisioningSession session, IProfileChangeRequest iProfileChangeRequest) {
		this(session, iProfileChangeRequest, null);

	}

	public RemediationOperation(ProvisioningSession session, IProfileChangeRequest originalRequest, List<RemedyConfig> configuration) {
		this(session, originalRequest, false);
	}

	public RemediationOperation(ProvisioningSession session, IProfileChangeRequest originalRequest, boolean isCheckForUpdates) {
		super(session);
		this.originalRequest = originalRequest;
		remedies = new ArrayList<Remedy>();
		this.isCheckForUpdates = isCheckForUpdates;
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
		if (currentRemedy != null) {
			request = currentRemedy.getRequest();
			return;
		}

		try {
			if (isCheckForUpdates)
				status.add(computeCheckForUpdates(monitor));
			else
				status.add(computeAllRemediations(monitor));
		} catch (OperationCanceledException e) {
			status.add(Status.CANCEL_STATUS);
		}
		if (!isCheckForUpdates)
			determineBestSolutions();
	}

	private IStatus computeCheckForUpdates(IProgressMonitor monitor) {
		RemedyConfig config = new RemedyConfig();
		config.allowDifferentVersion = true;
		config.allowInstalledRemoval = false;
		config.allowInstalledUpdate = true;
		config.allowPartialInstall = false;
		Remedy remedy = computeRemedy(config, monitor);
		if (remedy != null) {
			remedies.add(remedy);
		}
		return Status.OK_STATUS;
	}

	private IStatus computeAllRemediations(IProgressMonitor monitor) {
		RemedyConfig[] remedyConfigs = RemedyConfig.getAllRemdyConfigs();
		SubMonitor sub = SubMonitor.convert(monitor, remedyConfigs.length);
		sub.setTaskName("Looking for alternate solutions");
		List<Remedy> tmpRemedies = new ArrayList<Remedy>(remedyConfigs.length);
		try {
			for (int i = 0; i < remedyConfigs.length; i++) {
				sub.subTask(i + " out of " + remedyConfigs.length); //$NON-NLS-1$
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
		return Status.OK_STATUS;
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
				request = remedy.getRequest();
			}
		}
		if (bestSolutionChangingTheRequest != null)
			request = bestSolutionChangingTheRequest.getRequest();
		else if (bestSolutionChangingWhatIsInstalled != null)
			request = bestSolutionChangingWhatIsInstalled.getRequest();
	}

	private Remedy computeRemedy(RemedyConfig configuration, IProgressMonitor monitor) {
		Remedy remedy = new Remedy();
		remedy.setConfig(configuration);
		IPlanner planner = session.getPlanner();
		IProfile profile = session.getProfileRegistry().getProfile(profileId);
		//request = (ProfileChangeRequest) originalRequest.clone();
		RequestFlexer av = new RequestFlexer(planner);
		av.setAllowDifferentVersion(configuration.allowDifferentVersion);
		av.setAllowInstalledElementChange(configuration.allowInstalledUpdate);
		av.setAllowInstalledElementRemoval(configuration.allowInstalledRemoval);
		av.setAllowPartialInstall(configuration.allowPartialInstall);
		av.setProvisioningContext(getProvisioningContext());
		remedy.setRequest((ProfileChangeRequest) av.getChangeRequest(originalRequest, profile, monitor));
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
		//		planner.resolve();
		if (status.getSeverity() != IStatus.CANCEL && status.getSeverity() != IStatus.ERROR) {
			if (job.getProvisioningPlan() != null) {
				ProfileModificationJob pJob = new ProfileModificationJob(getProvisioningJobName(), session, profileId, job.getProvisioningPlan(), job.getActualProvisioningContext());
				pJob.setAdditionalProgressMonitor(monitor);
				return pJob;
			}
		}
		return null;
	}

	public boolean hasRemedies() {
		return (remedies != null && remedies.size() > 0);
	}

	public ProfileChangeRequest getOriginalRequest() {
		return (ProfileChangeRequest) originalRequest;
	}
}
