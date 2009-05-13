/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.osgi.util.NLS;

/**
 * Utility methods for clients using the provisioning UI
 * 
 * @since 3.4
 */
public class ProvisioningUtil {

	public static void addMetadataRepository(URI location, boolean notify) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		manager.addRepository(location);
		if (notify) {
			IProvisioningEventBus bus = ProvUIActivator.getDefault().getProvisioningEventBus();
			if (bus != null) {
				bus.publishEvent(new UIRepositoryEvent(location, IRepository.TYPE_METADATA, RepositoryEvent.ADDED));
			}
		}
	}

	public static String getMetadataRepositoryProperty(URI location, String key) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		return manager.getRepositoryProperty(location, key);
	}

	public static void setMetadataRepositoryProperty(URI location, String key, String value) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		manager.setRepositoryProperty(location, key, value);
	}

	public static boolean getMetadataRepositoryEnablement(URI location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			return false;
		return manager.isEnabled(location);
	}

	public static boolean getArtifactRepositoryEnablement(URI location) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			return false;
		return manager.isEnabled(location);
	}

	public static IMetadataRepository loadMetadataRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		IMetadataRepository repo = manager.loadRepository(location, monitor);
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

	public static IStatus validateMetadataRepositoryLocation(URI location, IProgressMonitor monitor) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		return manager.validateRepositoryLocation(location, monitor);
	}

	public static void removeMetadataRepository(URI location) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		manager.removeRepository(location);
		IProvisioningEventBus bus = ProvUIActivator.getDefault().getProvisioningEventBus();
		if (bus != null) {
			bus.publishEvent(new UIRepositoryEvent(location, IRepository.TYPE_METADATA, RepositoryEvent.REMOVED));
		}
	}

	public static void addArtifactRepository(URI location, boolean notify) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		manager.addRepository(location);
		if (notify) {
			IProvisioningEventBus bus = ProvUIActivator.getDefault().getProvisioningEventBus();
			if (bus != null) {
				bus.publishEvent(new UIRepositoryEvent(location, IRepository.TYPE_ARTIFACT, RepositoryEvent.ADDED));
			}
		}
	}

	public static String getArtifactRepositoryProperty(URI location, String key) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		return manager.getRepositoryProperty(location, key);
	}

	public static void setArtifactRepositoryProperty(URI location, String key, String value) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		manager.setRepositoryProperty(location, key, value);
	}

	public static IArtifactRepository loadArtifactRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		IArtifactRepository repo = manager.loadRepository(location, monitor);
		if (repo == null) {
			throw new ProvisionException(NLS.bind(ProvUIMessages.ProvisioningUtil_LoadRepositoryFailure, location));
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

	public static void removeArtifactRepository(URI location) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		manager.removeRepository(location);
		IProvisioningEventBus bus = ProvUIActivator.getDefault().getProvisioningEventBus();
		if (bus != null) {
			bus.publishEvent(new UIRepositoryEvent(location, IRepository.TYPE_ARTIFACT, RepositoryEvent.REMOVED));
		}
	}

	public static IProfile addProfile(String profileId, Map properties, IProgressMonitor monitor) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		return profileRegistry.addProfile(profileId, properties);
	}

	public static void removeProfile(String profileId, IProgressMonitor monitor) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		profileRegistry.removeProfile(profileId);
	}

	public static IProfile[] getProfiles() throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		return profileRegistry.getProfiles();
	}

	public static long[] getProfileTimestamps(String id) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		return profileRegistry.listProfileTimestamps(id);

	}

	public static IProfile getProfile(String id) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		return profileRegistry.getProfile(id);
	}

	public static IProfile getProfile(String id, long timestamp) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		return profileRegistry.getProfile(id, timestamp);
	}

	public static URI[] getMetadataRepositories(int flags) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		return manager.getKnownRepositories(flags);
	}

	public static void refreshMetadataRepositories(URI[] urls, IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		SubMonitor mon = SubMonitor.convert(monitor, urls.length * 100);
		for (int i = 0; i < urls.length; i++) {
			try {
				manager.refreshRepository(urls[i], mon.newChild(100));
			} catch (ProvisionException e) {
				//ignore problematic repositories when refreshing
			}
		}
	}

	public static URI[] getArtifactRepositories(int flags) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		return manager.getKnownRepositories(flags);
	}

	public static void refreshArtifactRepositories(URI[] urls, IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		SubMonitor mon = SubMonitor.convert(monitor, urls.length * 100);
		for (int i = 0; i < urls.length; i++) {
			manager.refreshRepository(urls[i], mon.newChild(100));
		}
	}

	/*
	 * Get the plan for the specified install operation
	 */
	public static ProvisioningPlan getProvisioningPlan(ProfileChangeRequest request, ProvisioningContext context, IProgressMonitor monitor) throws ProvisionException {
		try {
			return getPlanner().getProvisioningPlan(request, context, monitor);
		} catch (OperationCanceledException e) {
			return null;
		}
	}

	/*
	 * Get a plan for reverting to a specified profile snapshot
	 */
	public static ProvisioningPlan getRevertPlan(IProfile currentProfile, IProfile snapshot, IProgressMonitor monitor) throws ProvisionException {
		Assert.isNotNull(currentProfile);
		Assert.isNotNull(snapshot);
		return getPlanner().getDiffPlan(currentProfile, snapshot, monitor);
	}

	/*
	 * Get sizing info for the specified plan
	 */
	public static long getSize(ProvisioningPlan plan, String profileId, ProvisioningContext context, IProgressMonitor monitor) throws ProvisionException {
		// If there is nothing to size, return 0
		if (plan == null)
			return IIUElement.SIZE_NOTAPPLICABLE;
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
		return IIUElement.SIZE_UNAVAILABLE;
	}

	/**
	 * Perform the specified provisioning plan.
	 * 
	 * @param plan the plan to perform
	 * @param phaseSet the phase set to use
	 * @param profile the profile to be changed.  This parameter is now ignored.
	 * @param context the provisioning context to be used
	 * @param monitor the progress monitor 
	 * @return a status indicating the success of the plan
	 * @throws ProvisionException
	 * 
	 * @deprecated clients should use {@linkplain #performProvisioningPlan(ProvisioningPlan, PhaseSet, ProvisioningContext, IProgressMonitor)} 
	 * because the profile argument is now ignored.
	 */
	public static IStatus performProvisioningPlan(ProvisioningPlan plan, PhaseSet phaseSet, IProfile profile, ProvisioningContext context, IProgressMonitor monitor) throws ProvisionException {
		// ignore the profile, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=272355
		return performProvisioningPlan(plan, phaseSet, context, monitor);
	}

	public static IStatus performProvisioningPlan(ProvisioningPlan plan, PhaseSet phaseSet, ProvisioningContext context, IProgressMonitor monitor) throws ProvisionException {
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
			Configurator configChanger = (Configurator) ServiceHelper.getService(ProvUIActivator.getContext(), Configurator.class.getName());
			try {
				// TODO see https://bugs.eclipse.org/bugs/show_bug.cgi?id=274876
				ProvisioningOperationRunner.suppressRestart(true);
				configChanger.applyConfiguration();
				// We just applied the configuration so restart is no longer required.
				ProvisioningOperationRunner.clearRestartRequests();
			} catch (IOException e) {
				mon.done();
				return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_InstallPlanConfigurationError, e);
			} finally {
				ProvisioningOperationRunner.suppressRestart(false);
			}
		}
		return getEngine().perform(profile, set, plan.getOperands(), context, mon.newChild(500 - ticksUsed));
	}

	private static IEngine getEngine() throws ProvisionException {
		IEngine engine = (IEngine) ServiceHelper.getService(ProvUIActivator.getContext(), IEngine.SERVICE_NAME);
		if (engine == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoEngineFound);
		}
		return engine;
	}

	public static IPlanner getPlanner() throws ProvisionException {
		IPlanner planner = (IPlanner) ServiceHelper.getService(ProvUIActivator.getContext(), IPlanner.class.getName());
		if (planner == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoPlannerFound);
		}
		return planner;
	}

	public static IDirector getDirector() throws ProvisionException {
		IDirector director = (IDirector) ServiceHelper.getService(ProvUIActivator.getContext(), IDirector.class.getName());
		if (director == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoDirectorFound);
		}
		return director;
	}

	public static void setColocatedRepositoryEnablement(URI location, boolean enabled) {
		IMetadataRepositoryManager metaManager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (metaManager != null)
			metaManager.setEnabled(location, enabled);
		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (artifactManager != null)
			artifactManager.setEnabled(location, enabled);
	}

	public static boolean isCategory(IInstallableUnit iu) {
		String isCategory = iu.getProperty(IInstallableUnit.PROP_TYPE_CATEGORY);
		return isCategory != null && Boolean.valueOf(isCategory).booleanValue();
	}

	/**
	 * Perform the provisioning plan using a default context that contacts all repositories.
	 * @param plan the plan to perform
	 * @param phaseSet the phase set to use
	 * @param profile the profile to be changed
	 * @param monitor the progress monitor 
	 * @return a status indicating the success of the plan
	 * @throws ProvisionException
	 * 
	 * @deprecated clients should use {@linkplain #performProvisioningPlan(ProvisioningPlan, PhaseSet, IProfile, ProvisioningContext, IProgressMonitor)} 
	 * to explicitly establish a provisioning context.  Otherwise all repositories will be contacted
	 */
	public static IStatus performProvisioningPlan(ProvisioningPlan plan, PhaseSet phaseSet, IProfile profile, IProgressMonitor monitor) throws ProvisionException {
		PhaseSet set;
		if (phaseSet == null)
			set = new DefaultPhaseSet();
		else
			set = phaseSet;
		return getEngine().perform(profile, set, plan.getOperands(), new ProvisioningContext(), monitor);
	}
}
