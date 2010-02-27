/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 ******************************************************************************/

package org.eclipse.equinox.p2.operations;

import java.io.IOException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.operations.*;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.*;
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
	 * A constant indicating that there was nothing to size (there
	 * was no valid plan that could be used to compute
	 * size).
	 */
	public static final long SIZE_NOTAPPLICABLE = -3L;
	/**
	 * Indicates that the size is unavailable (an
	 * attempt was made to compute size but it failed)
	 */
	public static final long SIZE_UNAVAILABLE = -2L;
	/**
	 * Indicates that the size is currently unknown
	 */
	public static final long SIZE_UNKNOWN = -1L;

	/**
	 * A status code used to indicate that there were no updates found when
	 * looking for updates.
	 */
	public static final int STATUS_NOTHING_TO_UPDATE = IStatusCodes.NOTHING_TO_UPDATE;

	/**
	 * A status code used to indicate that a repository location was not valid.
	 */
	public static final int STATUS_INVALID_REPOSITORY_LOCATION = IStatusCodes.INVALID_REPOSITORY_LOCATION;

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
	public IAgentLocation getAgentLocation() {
		return (IAgentLocation) agent.getService(IAgentLocation.SERVICE_NAME);
	}

	/**
	 * Return the artifact repository manager for this session
	 * @return the repository manager
	 */
	public IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
	}

	/**
	 * Return the metadata repository manager for this session
	 * @return the repository manager
	 */
	public IMetadataRepositoryManager getMetadataRepositoryManager() {
		return (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
	}

	/**
	 * Return the profile registry for this session
	 * @return the profile registry
	 */
	public IProfileRegistry getProfileRegistry() {
		return (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
	}

	/**
	 * Return the provisioning engine for this session
	 * @return the provisioning engine
	 */
	public IEngine getEngine() {
		return (IEngine) agent.getService(IEngine.SERVICE_NAME);
	}

	/**
	 * Return the provisioning event bus used for dispatching events.
	 * @return the event bus
	 */
	public IProvisioningEventBus getProvisioningEventBus() {
		return (IProvisioningEventBus) agent.getService(IProvisioningEventBus.SERVICE_NAME);
	}

	/**
	 * Return the planner used for this session
	 * @return the planner
	 */
	public IPlanner getPlanner() {
		return (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
	}

	/**
	 * Get sizing information about the specified plan.
	 * 
	 * @param plan the provisioning plan
	 * @param context the provisioning context to be used for the sizing
	 * @param monitor the progress monitor
	 * 
	 * @return a long integer describing the disk size required for the provisioning plan.
	 * 
	 * @see #SIZE_UNKNOWN
	 * @see #SIZE_UNAVAILABLE
	 * @see #SIZE_NOTAPPLICABLE
	 */
	public long getSize(IProvisioningPlan plan, ProvisioningContext context, IProgressMonitor monitor) {
		// If there is nothing to size, return 0
		if (plan == null)
			return SIZE_NOTAPPLICABLE;
		if (countPlanElements(plan) == 0)
			return 0;
		long installPlanSize = 0;
		SubMonitor mon = SubMonitor.convert(monitor, 300);
		if (plan.getInstallerPlan() != null) {
			SizingPhaseSet sizingPhaseSet = new SizingPhaseSet();
			IStatus status = getEngine().perform(plan.getInstallerPlan(), sizingPhaseSet, mon.newChild(100));
			if (status.isOK())
				installPlanSize = sizingPhaseSet.getDiskSize();
		} else {
			mon.worked(100);
		}
		SizingPhaseSet sizingPhaseSet = new SizingPhaseSet();
		IStatus status = getEngine().perform(plan, sizingPhaseSet, mon.newChild(200));
		if (status.isOK())
			return installPlanSize + sizingPhaseSet.getDiskSize();
		return SIZE_UNAVAILABLE;
	}

	private int countPlanElements(IProvisioningPlan plan) {
		return QueryUtil.compoundQueryable(plan.getAdditions(), plan.getRemovals()).query(QueryUtil.createIUAnyQuery(), null).unmodifiableSet().size();
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
			set = new DefaultPhaseSet();
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
			if (set instanceof DefaultPhaseSet) {
				// If the phase set calls for download and install, then we want to download everything atomically before 
				// applying the install plan.  This way, we can be sure to install the install handler only if we know 
				// we will be able to get everything else.
				ProfileChangeRequest downloadRequest = new ProfileChangeRequest(profile);
				downloadRequest.setAbsoluteMode(true);
				downloadRequest.addAll(QueryUtil.compoundQueryable(plan.getAdditions(), plan.getInstallerPlan().getAdditions()).query(QueryUtil.createIUAnyQuery(), null).unmodifiableSet());

				IPhaseSet download = DefaultPhaseSet.createIncluding(new String[] {DefaultPhaseSet.PHASE_COLLECT});
				IProvisioningPlan downloadPlan = getPlanner().getProvisioningPlan(downloadRequest, context, mon.newChild(100));
				IStatus downloadStatus = getEngine().perform(downloadPlan, download, mon.newChild(300));
				if (!downloadStatus.isOK()) {
					mon.done();
					return downloadStatus;
				}
				ticksUsed = 300;
			}
			// we pre-downloaded if necessary.  Now perform the plan against the original phase set.
			IStatus installerPlanStatus = getEngine().perform(plan.getInstallerPlan(), set, mon.newChild(100));
			if (!installerPlanStatus.isOK()) {
				mon.done();
				return installerPlanStatus;
			}
			ticksUsed += 100;
			// Apply the configuration
			Configurator configChanger = (Configurator) ServiceHelper.getService(Activator.getContext(), Configurator.class.getName());
			try {
				configChanger.applyConfiguration();
			} catch (IOException e) {
				mon.done();
				return new Status(IStatus.ERROR, Activator.ID, Messages.ProvisioningSession_InstallPlanConfigurationError, e);
			}
		}
		return getEngine().perform(plan, set, mon.newChild(500 - ticksUsed));
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

	/**
	 * Get the IInstallable units for the specified profile
	 * 
	 * @param profileId the profile in question
	 * @param all <code>true</code> if all IInstallableUnits in the profile should
	 * be returned, <code>false</code> only those IInstallableUnits marked as (user visible) roots
	 * should be returned.
	 * 
	 * @return an array of IInstallableUnits installed in the profile.
	 */
	public Collection<IInstallableUnit> getInstalledIUs(String profileId, boolean all) {
		IProfile profile = getProfileRegistry().getProfile(profileId);
		if (profile == null)
			return CollectionUtils.emptyList();
		IQuery<IInstallableUnit> query;
		if (all)
			query = QueryUtil.createIUAnyQuery();
		else
			query = new UserVisibleRootQuery();
		IQueryResult<IInstallableUnit> queryResult = profile.query(query, null);
		return queryResult.unmodifiableSet();
	}
}
