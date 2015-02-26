/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *     Red Hat Inc. - Bug 460967
 ******************************************************************************/

package org.eclipse.equinox.p2.operations;

import java.io.IOException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.operations.Activator;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 * ProvisioningSession provides the context for a provisioning session, including
 * the provisioning services that should be used.  It also provides utility
 * methods for commonly performed provisioning tasks.
 * 
 * @since 2.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ProvisioningSession {
	private IProvisioningAgent agent;

	Set<Job> scheduledJobs = Collections.synchronizedSet(new HashSet<Job>());

	/**
	 * Create a provisioning session using the services of the supplied agent.
	 * @param agent the provisioning agent that supplies services.  Must not be <code>null</code>.
	 */
	public ProvisioningSession(IProvisioningAgent agent) {
		Assert.isNotNull(agent, Messages.ProvisioningSession_AgentNotFound);
		this.agent = agent;
	}

	/**
	 * Return the provisioning agent used to retrieve provisioning services.
	 * @return the provisioning agent
	 */
	public IProvisioningAgent getProvisioningAgent() {
		return agent;
	}

	/**
	 * Return the agent location for this session
	 * @return the agent location
	 */
	IAgentLocation getAgentLocation() {
		return (IAgentLocation) agent.getService(IAgentLocation.SERVICE_NAME);
	}

	/**
	 * Return the artifact repository manager for this session
	 * @return the repository manager
	 */
	IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
	}

	/**
	 * Return the metadata repository manager for this session
	 * @return the repository manager
	 */
	IMetadataRepositoryManager getMetadataRepositoryManager() {
		return (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
	}

	IProfileRegistry getProfileRegistry() {
		return (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
	}

	/**
	 * Return the provisioning engine for this session
	 * @return the provisioning engine
	 */
	IEngine getEngine() {
		return (IEngine) agent.getService(IEngine.SERVICE_NAME);
	}

	/**
	 * Return the provisioning event bus used for dispatching events.
	 * @return the event bus
	 */
	IProvisioningEventBus getProvisioningEventBus() {
		return (IProvisioningEventBus) agent.getService(IProvisioningEventBus.SERVICE_NAME);
	}

	/**
	 * Return the planner used for this session
	 * @return the planner
	 */
	IPlanner getPlanner() {
		return (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
	}

	/**
	 * Perform the specified provisioning plan.
	 * 
	 * @param plan the provisioning plan to be performed
	 * @param phaseSet the phase set to be used for the plan
	 * @param context the provisioning context to be used during provisioning
	 * @param monitor the progress monitor to use while performing the plan
	 * @return a status describing the result of performing the plan
	 */
	public IStatus performProvisioningPlan(IProvisioningPlan plan, IPhaseSet phaseSet, ProvisioningContext context, IProgressMonitor monitor) {
		IPhaseSet set;
		if (phaseSet == null)
			set = PhaseSetFactory.createDefaultPhaseSet();
		else
			set = phaseSet;

		// 300 ticks for download, 100 to install handlers, 100 to compute the plan, 100 to install the rest
		SubMonitor mon = SubMonitor.convert(monitor, 600);
		int ticksUsed = 0;

		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=272355
		// The exact profile instance used in the profile change request and passed to the engine must be used for all
		// of these operations, otherwise we can get profile out of synch errors.	
		IProfile profile = plan.getProfile();

		if (plan.getInstallerPlan() != null) {
			if (doesPhaseSetIncludeDownload(set)) {
				// If the phase set calls for download, then we want to download the install plan artifacts
				// at the same time as the actual install artifacts.  This way, we will only install the install handler
				// after already knowing we have successfully obtained the artifacts that will be installed afterward.
				IProvisioningPlan downloadPlan = getEngine().createPlan(profile, context);
				Iterator<IInstallableUnit> it = QueryUtil.compoundQueryable(plan.getAdditions(), plan.getInstallerPlan().getAdditions()).query(QueryUtil.createIUAnyQuery(), null).iterator();
				while (it.hasNext()) {
					downloadPlan.addInstallableUnit(it.next());
				}
				IPhaseSet download = PhaseSetFactory.createPhaseSetIncluding(new String[] {PhaseSetFactory.PHASE_COLLECT});
				IStatus downloadStatus = getEngine().perform(downloadPlan, download, mon.newChild(300));
				if (!downloadStatus.isOK()) {
					mon.done();
					return downloadStatus;
				}
				ticksUsed = 300;
			}
			// we pre-downloaded if necessary.  Now perform the install plan against the original phase set.
			IStatus installerPlanStatus = getEngine().perform(plan.getInstallerPlan(), set, mon.newChild(100));
			if (!installerPlanStatus.isOK()) {
				mon.done();
				return installerPlanStatus;
			}
			ticksUsed += 100;
			// Apply the configuration
			Configurator configChanger = ServiceHelper.getService(Activator.getContext(), Configurator.class);
			try {
				configChanger.applyConfiguration();
			} catch (IOException e) {
				mon.done();
				return new Status(IStatus.ERROR, Activator.ID, Messages.ProvisioningSession_InstallPlanConfigurationError, e);
			}
		}
		return getEngine().perform(plan, set, mon.newChild(500 - ticksUsed));
	}

	private boolean doesPhaseSetIncludeDownload(IPhaseSet set) {
		String[] phaseIds = set.getPhaseIds();
		for (int i = 0; i < phaseIds.length; i++)
			if (phaseIds[i].equals(PhaseSetFactory.PHASE_COLLECT))
				return true;
		return false;
	}

	/**
	 * Return a boolean indicating whether any other provisioning operations are
	 * scheduled for the specified profile.
	 * 
	 * @param profileId the id of the profile in question
	 * @return <code>true</code> if there are pending provisioning operations for
	 * this profile, <code>false</code> if there are not.
	 * @see #rememberJob(Job)
	 */
	public boolean hasScheduledOperationsFor(String profileId) {
		Job[] jobs = getScheduledJobs();
		for (int i = 0; i < jobs.length; i++) {
			if (jobs[i] instanceof IProfileChangeJob) {
				String id = ((IProfileChangeJob) jobs[i]).getProfileId();
				if (profileId.equals(id))
					return true;
			}
		}
		return false;
	}

	private Job[] getScheduledJobs() {
		synchronized (scheduledJobs) {
			return scheduledJobs.toArray(new Job[scheduledJobs.size()]);
		}
	}

	/**
	 * Remember the specified job.  Remembered jobs are
	 * checked when callers want to know what work is scheduled for
	 * a particular profile.
	 * 
	 * @param job the job to be remembered
	 * @see #hasScheduledOperationsFor(String)
	 */
	public void rememberJob(Job job) {
		scheduledJobs.add(job);
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				scheduledJobs.remove(event.getJob());
			}
		});
	}
}
