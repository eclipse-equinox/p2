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

import java.io.IOException;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.operations.*;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.osgi.util.NLS;

/**
 * ProvisioningSession provides the context for a provisioning session, including
 * the profile that is being provisioned, the repository managers in use, the
 * provisioning engine, and the planner.
 * 
 * @since 2.0
 * @noextend This class is not intended to be subclassed by clients.
 *
 */
public class ProvisioningSession {
	private IProvisioningAgent agent;

	HashSet scheduledJobs = new HashSet();

	/**
	 * Indicates that there was nothing to size (there
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

	public static final int STATUS_NOTHING_TO_UPDATE = IStatusCodes.NOTHING_TO_UPDATE;
	public static final int STATUS_INVALID_REPOSITORY_LOCATION = IStatusCodes.INVALID_REPOSITORY_LOCATION;

	public ProvisioningSession(IProvisioningAgent agent) {
		Assert.isNotNull(agent, Messages.ProvisioningSession_AgentNotFound);
		this.agent = agent;
	}

	public IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
	}

	public IMetadataRepositoryManager getMetadataRepositoryManager() {
		return (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
	}

	public IProfileRegistry getProfileRegistry() {
		return (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
	}

	public IEngine getEngine() {
		return (IEngine) agent.getService(IEngine.SERVICE_NAME);
	}

	public IProvisioningEventBus getProvisioningEventBus() {
		return (IProvisioningEventBus) agent.getService(IProvisioningEventBus.SERVICE_NAME);
	}

	public IPlanner getPlanner() {
		return (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
	}

	public IMetadataRepository loadMetadataRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepository repo;
		try {
			signalOperationStart();
			repo = getMetadataRepositoryManager().loadRepository(location, monitor);
			// If there is no user nickname assigned to this repo but there is a provider name, then set the nickname.
			// This will keep the name in the manager even when the repo is not loaded
			String name = getMetadataRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NICKNAME);
			if (name == null || name.length() == 0) {
				name = repo.getName();
				if (name != null && name.length() > 0)
					getMetadataRepositoryManager().setRepositoryProperty(location, IRepository.PROP_NICKNAME, name);
			}
		} finally {
			signalOperationComplete(location);
		}
		return repo;
	}

	public void refreshMetadataRepositories(URI[] urls, IProgressMonitor monitor) {
		signalOperationStart();
		SubMonitor mon = SubMonitor.convert(monitor, urls.length * 100);
		for (int i = 0; i < urls.length; i++) {
			try {
				getMetadataRepositoryManager().refreshRepository(urls[i], mon.newChild(100));
			} catch (ProvisionException e) {
				//ignore problematic repositories when refreshing
			}
		}
		if (urls.length == 1)
			signalOperationComplete(urls[0]);
		else
			signalOperationComplete(null);
	}

	public IArtifactRepository loadArtifactRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepository repo = getArtifactRepositoryManager().loadRepository(location, monitor);
		if (repo == null) {
			throw new ProvisionException(NLS.bind(Messages.ProvisioningSession_LoadRepositoryFailure, location));
		}
		// If there is no user nickname assigned to this repo but there is a provider name, then set the nickname.
		// This will keep the name in the manager even when the repo is not loaded
		String name = getArtifactRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NICKNAME);
		if (name == null) {
			name = getArtifactRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NAME);
			if (name != null)
				getArtifactRepositoryManager().setRepositoryProperty(location, IRepository.PROP_NICKNAME, name);
		}
		return repo;
	}

	public void refreshArtifactRepositories(URI[] urls, IProgressMonitor monitor) throws ProvisionException {
		SubMonitor mon = SubMonitor.convert(monitor, urls.length * 100);
		for (int i = 0; i < urls.length; i++) {
			getArtifactRepositoryManager().refreshRepository(urls[i], mon.newChild(100));
		}
	}

	public IProfile getProfile(String id) {
		return getProfileRegistry().getProfile(id);
	}

	/*
	 * Get the plan for the specified install operation
	 */
	public IProvisioningPlan getProvisioningPlan(ProfileChangeRequest request, ProvisioningContext context, IProgressMonitor monitor) {
		try {
			return getPlanner().getProvisioningPlan(request, context, monitor);
		} catch (OperationCanceledException e) {
			return null;
		}
	}

	/*
	 * Get a plan for reverting to a specified profile snapshot
	 */
	public IProvisioningPlan getRevertPlan(IProfile currentProfile, IProfile snapshot, IProgressMonitor monitor) {
		Assert.isNotNull(currentProfile);
		Assert.isNotNull(snapshot);
		return getPlanner().getDiffPlan(currentProfile, snapshot, monitor);
	}

	/*
	 * Get sizing info for the specified plan
	 */
	public long getSize(IProvisioningPlan plan, String profileId, ProvisioningContext context, IProgressMonitor monitor) {
		// If there is nothing to size, return 0
		if (plan == null)
			return SIZE_NOTAPPLICABLE;
		if (plan.getOperands().length == 0)
			return 0;
		long installPlanSize = 0;
		SubMonitor mon = SubMonitor.convert(monitor, 300);
		if (plan.getInstallerPlan() != null) {
			SizingPhaseSet set = new SizingPhaseSet();
			IStatus status = getEngine().perform(getProfile(profileId), set, plan.getInstallerPlan().getOperands(), context, mon.newChild(100));
			if (status.isOK())
				installPlanSize = set.getSizing().getDiskSize();
		} else {
			mon.worked(100);
		}
		SizingPhaseSet set = new SizingPhaseSet();
		IStatus status = getEngine().perform(getProfile(profileId), set, plan.getOperands(), context, mon.newChild(200));
		if (status.isOK())
			return installPlanSize + set.getSizing().getDiskSize();
		return SIZE_UNAVAILABLE;
	}

	public IStatus performProvisioningPlan(IProvisioningPlan plan, PhaseSet phaseSet, ProvisioningContext context, IProgressMonitor monitor) throws ProvisionException {
		PhaseSet set;
		if (phaseSet == null)
			set = new DefaultPhaseSet();
		else
			set = phaseSet;

		// 300 ticks for download, 100 to install handlers, 100 to install the rest
		SubMonitor mon = SubMonitor.convert(monitor, 500);
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
				List allOperands = new ArrayList();
				allOperands.addAll(Arrays.asList(plan.getOperands()));
				allOperands.addAll(Arrays.asList(plan.getInstallerPlan().getOperands()));
				PhaseSet download = new DownloadPhaseSet();
				IStatus downloadStatus = getEngine().perform(profile, download, (Operand[]) allOperands.toArray(new Operand[allOperands.size()]), context, mon.newChild(300));
				if (!downloadStatus.isOK()) {
					mon.done();
					return downloadStatus;
				}
				ticksUsed = 300;
			}
			// we pre-downloaded if necessary.  Now perform the plan against the original phase set.
			IStatus installerPlanStatus = getEngine().perform(profile, set, plan.getInstallerPlan().getOperands(), context, mon.newChild(100));
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
		return getEngine().perform(profile, set, plan.getOperands(), context, mon.newChild(500 - ticksUsed));
	}

	public void signalOperationStart() {
		getProvisioningEventBus().publishEvent(new OperationBeginningEvent(this));
	}

	public void signalOperationComplete(Object item) {
		getProvisioningEventBus().publishEvent(new OperationEndingEvent(this, item));
	}

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
		return (Job[]) scheduledJobs.toArray(new Job[scheduledJobs.size()]);
	}

	public void manageJob(Job job) {
		scheduledJobs.add(job);
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				scheduledJobs.remove(event.getJob());
			}
		});
	}

	public IInstallableUnit[] getProfileRoots(String profileId, boolean all) {
		IProfile profile = this.getProfile(profileId);
		if (profile == null)
			return new IInstallableUnit[0];
		IQuery query;
		if (all)
			query = InstallableUnitQuery.ANY;
		else
			query = new UserVisibleRootQuery();
		Collector collector = profile.query(query, new Collector(), null);
		return (IInstallableUnit[]) collector.toArray(IInstallableUnit.class);
	}

}
