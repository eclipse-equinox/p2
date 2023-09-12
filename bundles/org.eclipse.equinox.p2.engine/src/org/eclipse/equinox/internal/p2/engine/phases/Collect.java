/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     WindRiver - https://bugs.eclipse.org/bugs/show_bug.cgi?id=227372
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine.phases;

import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.p2.repository.DownloadPauseResumeEvent;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ITouchpointType;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.osgi.util.NLS;

/**
 * The goal of the collect phase is to ask the touchpoints if the artifacts associated with an IU need to be downloaded.
 */
public class Collect extends InstallableUnitPhase {
	public static final String PARM_ARTIFACT_REQUESTS = "artifactRequests"; //$NON-NLS-1$
	public static final String NO_ARTIFACT_REPOSITORIES_AVAILABLE = "noArtifactRepositoriesAvailable"; //$NON-NLS-1$
	private static final String PARM_IUS = "ius"; //$NON-NLS-1$
	private IProvisioningAgent agent = null;

	public Collect(int weight) {
		super(PhaseSetFactory.PHASE_COLLECT, weight);
		//re-balance work since postPerform will do almost all the time-consuming work
		prePerformWork = 0;
		mainPerformWork = 100;
		postPerformWork = 1000;
	}

	@Override
	protected boolean isApplicable(InstallableUnitOperand op) {
		return (op.second() != null && !op.second().equals(op.first()));
	}

	@Override
	protected List<ProvisioningAction> getActions(InstallableUnitOperand operand) {
		IInstallableUnit unit = operand.second();
		List<ProvisioningAction> parsedActions = getActions(unit, phaseId);
		if (parsedActions != null)
			return parsedActions;

		ITouchpointType type = unit.getTouchpointType();
		if (type == null || type == ITouchpointType.NONE)
			return null;

		String actionId = getActionManager().getTouchpointQualifiedActionId(phaseId, type);
		ProvisioningAction action = getActionManager().getAction(actionId, null);
		if (action == null) {
			return null;
		}
		return Collections.singletonList(action);
	}

	@Override
	protected String getProblemMessage() {
		return Messages.Phase_Collect_Error;
	}

	@Override
	protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
		// do nothing for rollback if the provisioning has been cancelled
		if (monitor.isCanceled())
			return Status.OK_STATUS;
		@SuppressWarnings("unchecked")
		List<IArtifactRequest[]> artifactRequests = (List<IArtifactRequest[]>) parameters.get(PARM_ARTIFACT_REQUESTS);
		// it happens when rollbacking
		if (artifactRequests.size() == 0)
			return Status.OK_STATUS;
		ProvisioningContext context = (ProvisioningContext) parameters.get(PARM_CONTEXT);
		synchronized (this) {
			agent = (IProvisioningAgent) parameters.get(PARM_AGENT);
		}

		if (Boolean.parseBoolean(context.getProperty(ProvisioningContext.CHECK_AUTHORITIES))) {
			@SuppressWarnings("unchecked")
			Set<IInstallableUnit> ius = (Set<IInstallableUnit>) parameters.get(PARM_IUS);
			IStatus authorityStatus = new AuthorityChecker(agent, context, ius, artifactRequests.stream()
						.flatMap(Arrays::stream).map(IArtifactRequest::getArtifactKey).collect(Collectors.toList()),
						profile).start(monitor);
			if (!authorityStatus.isOK()) {
				return authorityStatus;
			}
		}

		if (isPaused) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.phase_thread_interrupted_error, phaseId), e);
			}
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
		}

		List<IArtifactRequest> totalArtifactRequests = new ArrayList<>(artifactRequests.size());
		DownloadManager dm = new DownloadManager(context, agent);
		for (IArtifactRequest[] requests : artifactRequests) {
			for (IArtifactRequest request : requests) {
				dm.add(request);
				totalArtifactRequests.add(request);
			}
		}
		IProvisioningEventBus bus = agent.getService(IProvisioningEventBus.class);
		if (bus != null)
			bus.publishEvent(new CollectEvent(CollectEvent.TYPE_OVERALL_START, null, context, totalArtifactRequests.toArray(new IArtifactRequest[totalArtifactRequests.size()])));
		IStatus downloadStatus = dm.start(monitor);
		try {
			return downloadStatus;
		} finally {
			if (downloadStatus.isOK() && bus != null)
				bus.publishEvent(new CollectEvent(CollectEvent.TYPE_OVERALL_END, null, context, totalArtifactRequests.toArray(new IArtifactRequest[totalArtifactRequests.size()])));
			synchronized (this) {
				agent = null;
			}
		}
	}

	@Override
	protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
		parameters.put(PARM_ARTIFACT_REQUESTS, new ArrayList<>());
		parameters.put(PARM_IUS, new HashSet<>());
		return null;
	}

	@Override
	protected void setPaused(boolean isPaused) {
		super.setPaused(isPaused);
		firePauseEventToDownloadJobs();
	}

	private void firePauseEventToDownloadJobs() {
		synchronized (this) {
			if (agent != null) {
				IProvisioningEventBus bus = agent.getService(IProvisioningEventBus.class);
				if (bus != null)
					bus.publishEvent(new DownloadPauseResumeEvent(isPaused ? DownloadPauseResumeEvent.TYPE_PAUSE : DownloadPauseResumeEvent.TYPE_RESUME));
			}
		}
	}

	@Override
	protected IStatus initializeOperand(IProfile profile, InstallableUnitOperand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
		IStatus status = super.initializeOperand(profile, operand, parameters, monitor);
		// defer setting the IU until after the super method to avoid triggering touchpoint initialization
		IInstallableUnit iu = operand.second();
		parameters.put(PARM_IU, iu);
		@SuppressWarnings("unchecked")
		Set<IInstallableUnit> ius = (Set<IInstallableUnit>) parameters.get(PARM_IUS);
		ius.add(iu);
		return status;
	}

}
