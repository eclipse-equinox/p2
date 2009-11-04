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
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.operations.*;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
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
	protected IProfileRegistry profileRegistry;
	protected IPlanner planner;
	protected IEngine engine;
	protected IMetadataRepositoryManager metadataRepositoryManager;
	protected IArtifactRepositoryManager artifactRepositoryManager;
	private IProvisioningEventBus eventBus;

	public ProvisioningSession(IProfileRegistry profileRegistry, IPlanner planner, IEngine engine, IMetadataRepositoryManager metadataRepositoryManager, IArtifactRepositoryManager artifactRepositoryManager, IProvisioningEventBus eventBus) {
		Assert.isNotNull(profileRegistry, Messages.ProvisioningSession_NoProfileRegistryFound);
		Assert.isNotNull(engine, Messages.ProvisioningSession_NoEngineFound);
		Assert.isNotNull(planner, Messages.ProvisioningSession_NoPlannerFound);
		Assert.isNotNull(metadataRepositoryManager, Messages.ProvisioningSession_NoRepositoryManager);
		Assert.isNotNull(artifactRepositoryManager, Messages.ProvisioningSession_NoRepositoryManager);
		Assert.isNotNull(eventBus, Messages.ProvisioningSession_NoEventBus);
		this.profileRegistry = profileRegistry;
		this.planner = planner;
		this.engine = engine;
		this.metadataRepositoryManager = metadataRepositoryManager;
		this.artifactRepositoryManager = artifactRepositoryManager;
		this.eventBus = eventBus;
	}

	public IArtifactRepositoryManager getArtifactRepositoryManager() {
		return artifactRepositoryManager;
	}

	public IMetadataRepositoryManager getMetadataRepositoryManager() {
		return metadataRepositoryManager;
	}

	public IProfileRegistry getProfileRegistry() {
		return profileRegistry;
	}

	public IPlanner getPlanner() {
		return planner;
	}

	public void addMetadataRepository(URI location) {
		metadataRepositoryManager.addRepository(location);
	}

	public String getMetadataRepositoryProperty(URI location, String key) {
		return metadataRepositoryManager.getRepositoryProperty(location, key);
	}

	public void setMetadataRepositoryProperty(URI location, String key, String value) {
		metadataRepositoryManager.setRepositoryProperty(location, key, value);
	}

	public boolean getMetadataRepositoryEnablement(URI location) {
		return metadataRepositoryManager.isEnabled(location);
	}

	public void setMetadataRepositoryEnablement(URI location, boolean enabled) {
		metadataRepositoryManager.setEnabled(location, enabled);
	}

	public IMetadataRepository loadMetadataRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepository repo = metadataRepositoryManager.loadRepository(location, monitor);
		// If there is no user nickname assigned to this repo but there is a provider name, then set the nickname.
		// This will keep the name in the manager even when the repo is not loaded
		String name = getMetadataRepositoryProperty(location, IRepository.PROP_NICKNAME);
		if (name == null || name.length() == 0) {
			name = repo.getName();
			if (name != null && name.length() > 0)
				setMetadataRepositoryProperty(location, IRepository.PROP_NICKNAME, name);
		}
		return repo;
	}

	public IStatus validateMetadataRepositoryLocation(URI location, IProgressMonitor monitor) {
		return metadataRepositoryManager.validateRepositoryLocation(location, monitor);
	}

	public void removeMetadataRepository(URI location) {
		metadataRepositoryManager.removeRepository(location);
	}

	public URI[] getMetadataRepositories(int flags) {
		return metadataRepositoryManager.getKnownRepositories(flags);
	}

	public void refreshMetadataRepositories(URI[] urls, IProgressMonitor monitor) {
		SubMonitor mon = SubMonitor.convert(monitor, urls.length * 100);
		for (int i = 0; i < urls.length; i++) {
			try {
				metadataRepositoryManager.refreshRepository(urls[i], mon.newChild(100));
			} catch (ProvisionException e) {
				//ignore problematic repositories when refreshing
			}
		}
	}

	public boolean getArtifactRepositoryEnablement(URI location) {
		return artifactRepositoryManager.isEnabled(location);
	}

	public void setArtifactRepositoryEnablement(URI location, boolean enabled) {
		artifactRepositoryManager.setEnabled(location, enabled);
	}

	public void addArtifactRepository(URI location) {
		artifactRepositoryManager.addRepository(location);
	}

	public String getArtifactRepositoryProperty(URI location, String key) {
		return artifactRepositoryManager.getRepositoryProperty(location, key);
	}

	public void setArtifactRepositoryProperty(URI location, String key, String value) {
		artifactRepositoryManager.setRepositoryProperty(location, key, value);
	}

	public IArtifactRepository loadArtifactRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepository repo = artifactRepositoryManager.loadRepository(location, monitor);
		if (repo == null) {
			throw new ProvisionException(NLS.bind(Messages.ProvisioningSession_LoadRepositoryFailure, location));
		}
		// If there is no user nickname assigned to this repo but there is a provider name, then set the nickname.
		// This will keep the name in the manager even when the repo is not loaded
		String name = getArtifactRepositoryProperty(location, IRepository.PROP_NICKNAME);
		if (name == null) {
			name = getArtifactRepositoryProperty(location, IRepository.PROP_NAME);
			if (name != null)
				setArtifactRepositoryProperty(location, IRepository.PROP_NICKNAME, name);
		}
		return repo;
	}

	public void removeArtifactRepository(URI location) {
		artifactRepositoryManager.removeRepository(location);
	}

	public URI[] getArtifactRepositories(int flags) {
		return artifactRepositoryManager.getKnownRepositories(flags);
	}

	public void refreshArtifactRepositories(URI[] urls, IProgressMonitor monitor) throws ProvisionException {
		SubMonitor mon = SubMonitor.convert(monitor, urls.length * 100);
		for (int i = 0; i < urls.length; i++) {
			artifactRepositoryManager.refreshRepository(urls[i], mon.newChild(100));
		}
	}

	public IProfile addProfile(String profileId, Map properties, IProgressMonitor monitor) throws ProvisionException {
		return profileRegistry.addProfile(profileId, properties);
	}

	public void removeProfile(String profileId, IProgressMonitor monitor) {
		profileRegistry.removeProfile(profileId);
	}

	public IProfile[] getProfiles() {
		return profileRegistry.getProfiles();
	}

	public long[] getProfileTimestamps(String id) {
		return profileRegistry.listProfileTimestamps(id);
	}

	public IProfile getProfile(String id) {
		return profileRegistry.getProfile(id);
	}

	public IProfile getProfile(String id, long timestamp) {
		return profileRegistry.getProfile(id, timestamp);
	}

	/*
	 * Get the plan for the specified install operation
	 */
	public ProvisioningPlan getProvisioningPlan(ProfileChangeRequest request, ProvisioningContext context, IProgressMonitor monitor) {
		try {
			return planner.getProvisioningPlan(request, context, monitor);
		} catch (OperationCanceledException e) {
			return null;
		}
	}

	/*
	 * Get a plan for reverting to a specified profile snapshot
	 */
	public ProvisioningPlan getRevertPlan(IProfile currentProfile, IProfile snapshot, IProgressMonitor monitor) {
		Assert.isNotNull(currentProfile);
		Assert.isNotNull(snapshot);
		return planner.getDiffPlan(currentProfile, snapshot, monitor);
	}

	/*
	 * Get sizing info for the specified plan
	 */
	public long getSize(ProvisioningPlan plan, String profileId, ProvisioningContext context, IProgressMonitor monitor) {
		// If there is nothing to size, return 0
		if (plan == null)
			return SizingPhaseSet.SIZE_NOTAPPLICABLE;
		if (plan.getOperands().length == 0)
			return 0;
		long installPlanSize = 0;
		SubMonitor mon = SubMonitor.convert(monitor, 300);
		if (plan.getInstallerPlan() != null) {
			SizingPhaseSet set = new SizingPhaseSet();
			IStatus status = engine.perform(getProfile(profileId), set, plan.getInstallerPlan().getOperands(), context, mon.newChild(100));
			if (status.isOK())
				installPlanSize = set.getSizing().getDiskSize();
		} else {
			mon.worked(100);
		}
		SizingPhaseSet set = new SizingPhaseSet();
		IStatus status = engine.perform(getProfile(profileId), set, plan.getOperands(), context, mon.newChild(200));
		if (status.isOK())
			return installPlanSize + set.getSizing().getDiskSize();
		return SizingPhaseSet.SIZE_UNAVAILABLE;
	}

	public IStatus performProvisioningPlan(ProvisioningPlan plan, PhaseSet phaseSet, ProvisioningContext context, IProgressMonitor monitor) throws ProvisionException {
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
		IProfile profile = plan.getProfileChangeRequest().getProfile();

		if (plan.getInstallerPlan() != null) {
			if (set instanceof DefaultPhaseSet) {
				// If the phase set calls for download and install, then we want to download everything atomically before 
				// applying the install plan.  This way, we can be sure to install the install handler only if we know 
				// we will be able to get everything else.
				List allOperands = new ArrayList();
				allOperands.addAll(Arrays.asList(plan.getOperands()));
				allOperands.addAll(Arrays.asList(plan.getInstallerPlan().getOperands()));
				PhaseSet download = new DownloadPhaseSet();
				IStatus downloadStatus = engine.perform(profile, download, (Operand[]) allOperands.toArray(new Operand[allOperands.size()]), context, mon.newChild(300));
				if (!downloadStatus.isOK()) {
					mon.done();
					return downloadStatus;
				}
				ticksUsed = 300;
			}
			// we pre-downloaded if necessary.  Now perform the plan against the original phase set.
			IStatus installerPlanStatus = engine.perform(profile, set, plan.getInstallerPlan().getOperands(), context, mon.newChild(100));
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
		return engine.perform(profile, set, plan.getOperands(), context, mon.newChild(500 - ticksUsed));
	}

	public void signalBatchOperationStart() {
		eventBus.publishEvent(new OperationBeginningEvent(this));
	}

	public void signalBatchOperationComplete(boolean notify, EventObject lastEvent) {
		eventBus.publishEvent(new OperationEndingEvent(this, lastEvent, notify));
	}
}
