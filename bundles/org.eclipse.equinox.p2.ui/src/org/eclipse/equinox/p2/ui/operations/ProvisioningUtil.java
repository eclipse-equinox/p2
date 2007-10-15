/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.ui.operations;

import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.director.IPlanner;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.phases.Sizing;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitConstants;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.query.CompoundIterator;
import org.eclipse.equinox.p2.query.Query;
import org.eclipse.equinox.p2.ui.IProvisioningListener;
import org.eclipse.equinox.p2.ui.ProvUIActivator;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;

/**
 * Utility methods for clients using the provisioning UI
 * 
 * @since 3.4
 */
public class ProvisioningUtil {

	private static final class SizingPhaseSet extends PhaseSet {
		static Sizing sizing;

		SizingPhaseSet() {
			super(new Phase[] {sizing = new Sizing(100, "Compute sizes")}); //$NON-NLS-1$
		}

		Sizing getSizing() {
			return sizing;
		}
	}

	public static IMetadataRepository addMetadataRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		IMetadataRepository repo = manager.loadRepository(location, monitor);
		if (repo == null) {
			throw new ProvisionException(NLS.bind(ProvUIMessages.ProvisioningUtil_AddRepositoryFailure, location.toExternalForm()));
		}
		EventObject event = new EventObject(IProvisioningListener.REPO_ADDED);
		ProvUIActivator.getDefault().notifyListeners(event);
		return repo;
	}

	public static IMetadataRepository createMetadataRepository(String name, String type, URL location, IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		IMetadataRepository repo = manager.createRepository(location, name, type);
		if (repo == null) {
			throw new ProvisionException(NLS.bind(ProvUIMessages.ProvisioningUtil_CreateRepositoryFailure, location.toExternalForm()));
		}
		EventObject event = new EventObject(IProvisioningListener.REPO_ADDED);
		ProvUIActivator.getDefault().notifyListeners(event);
		return repo;
	}

	public static void removeMetadataRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		IMetadataRepository repo = manager.getRepository(location);
		if (repo != null)
			manager.removeRepository(repo);
		PropertyChangeEvent event = new PropertyChangeEvent(repo, IProvisioningListener.REPO_REMOVED, null, null);
		ProvUIActivator.getDefault().notifyListeners(event);

	}

	public static IArtifactRepository addArtifactRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		IArtifactRepository repo = manager.loadRepository(location, monitor);
		if (repo == null) {
			throw new ProvisionException(NLS.bind(ProvUIMessages.ProvisioningUtil_AddRepositoryFailure, location));
		}
		EventObject event = new EventObject(IProvisioningListener.REPO_ADDED);
		ProvUIActivator.getDefault().notifyListeners(event);

		return repo;
	}

	public static IArtifactRepository createArtifactRepository(String name, String type, URL location, IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		IArtifactRepository repo = manager.createRepository(location, name, type);
		if (repo == null) {
			throw new ProvisionException(NLS.bind(ProvUIMessages.ProvisioningUtil_CreateRepositoryFailure, location));
		}
		EventObject event = new EventObject(IProvisioningListener.REPO_ADDED);
		ProvUIActivator.getDefault().notifyListeners(event);

		return repo;
	}

	public static void removeArtifactRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		IArtifactRepository[] repos = manager.getKnownRepositories();
		for (int i = 0; i < repos.length; i++) {
			IArtifactRepository repo = repos[i];
			if (repo.getLocation().equals(location)) {
				manager.removeRepository(repo);
				PropertyChangeEvent event = new PropertyChangeEvent(repo, IProvisioningListener.REPO_REMOVED, null, null);
				ProvUIActivator.getDefault().notifyListeners(event);

				return;
			}
		}
	}

	public static IArtifactRepository[] getArtifactRepositories(IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		IArtifactRepository[] repos = manager.getKnownRepositories();
		if (repos != null) {
			return repos;
		}
		return new IArtifactRepository[0];
	}

	public static void addProfile(Profile profile, IProgressMonitor monitor) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		profileRegistry.addProfile(profile);
	}

	public static void removeProfile(String profileId, IProgressMonitor monitor) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		Profile profile = profileRegistry.getProfile(profileId);
		if (profile != null)
			profileRegistry.removeProfile(profile);
	}

	public static Profile[] getProfiles(IProgressMonitor monitor) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		return profileRegistry.getProfiles();
	}

	public static Profile getProfile(String id) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		return profileRegistry.getProfile(id);
	}

	/*
	 * Returns the installable units with the given id and version
	 * specifications in the given metadata repository. <code>null</code> can
	 * be used to indicate wildcards for any of the arguments.
	 * 
	 * @param location The location of the metdata repo to search. <code>null</code>
	 * indicates search all known repos. @param id The id of the IUs to find.
	 * <code>null</code> indicates wildcard. @param range The version range of
	 * the IUs to find. <code>null</code> indicates wildcard. @return The IUs
	 * that match the query
	 */
	public static IInstallableUnit[] getInstallableUnits(URL location, String id, VersionRange range, IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepository[] repositories = null;
		if (location == null) {
			repositories = getMetadataRepositories(monitor);
		} else {
			repositories = new IMetadataRepository[] {getMetadataRepository(location, monitor)};
		}
		Iterator i = Query.getIterator(repositories, id, range, null, false);
		return CompoundIterator.asArray(i, monitor);
	}

	/*
	 * Returns the installable units with the given id and version
	 * specifications.
	 * 
	 * @param profileId The profile to search @param id The id of the IUs to
	 * find. <code>null</code> indicates wildcard. @param version The version
	 * of the IUs to find. <code>null</code> indicates wildcard. @return The
	 * IUs that match the query
	 */
	public static IInstallableUnit[] getInstallableUnits(String profileId, String id, VersionRange range, IProgressMonitor monitor) throws ProvisionException {
		Profile[] profiles = null;
		if (profileId == null) {
			profiles = getProfiles(monitor);
		} else {
			profiles = new Profile[] {getProfile(profileId)};
		}
		Iterator i = Query.getIterator(profiles, id, range, null, false);
		return CompoundIterator.asArray(i, monitor);
	}

	public static IMetadataRepository[] getMetadataRepositories(IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		return manager.getKnownRepositories();
	}

	/*
	 * Get the plan for the specified install operation
	 */
	public static ProvisioningPlan getInstallPlan(IInstallableUnit[] toInstall, Profile profile, IProgressMonitor monitor) throws ProvisionException {
		Assert.isNotNull(profile);
		Assert.isNotNull(toInstall);
		return getPlanner().getInstallPlan(toInstall, profile, monitor);
	}

	/*
	 * Get the plan for the specified update operation
	 */
	public static ProvisioningPlan getReplacePlan(IInstallableUnit[] toUninstall, IInstallableUnit[] replacements, Profile profile, IProgressMonitor monitor) throws ProvisionException {
		Assert.isNotNull(profile);
		Assert.isNotNull(toUninstall);
		Assert.isNotNull(replacements);
		return getPlanner().getReplacePlan(toUninstall, replacements, profile, monitor);
	}

	/*
	 * See what updates might be available for a single IU.
	 * Useful when checking for updates and letting the user decide
	 * which IU's to update.
	 */
	public static IInstallableUnit[] updatesFor(IInstallableUnit toUpdate, IProgressMonitor monitor) throws ProvisionException {
		Assert.isNotNull(toUpdate);
		return getPlanner().updatesFor(toUpdate);
	}

	/*
	 * See what updates might be available for the specified IU's.
	 * Useful for bulk update that can be directly passed to the engine.
	 */
	public static IInstallableUnit[] updatesFor(IInstallableUnit[] toUpdate, Profile profile, IProgressMonitor monitor) throws ProvisionException {
		Assert.isNotNull(profile);
		Assert.isNotNull(toUpdate);

		IPlanner planner = getPlanner();
		ArrayList allUpdates = new ArrayList();
		for (int i = 0; i < toUpdate.length; i++) {
			IInstallableUnit[] updates = planner.updatesFor(toUpdate[i]);
			for (int j = 0; j < updates.length; j++)
				allUpdates.add(updates[j]);
		}
		return (IInstallableUnit[]) allUpdates.toArray(new IInstallableUnit[allUpdates.size()]);
	}

	/*
	 * Get a plan for becoming
	 */
	public static ProvisioningPlan getBecomePlan(IInstallableUnit toBecome, Profile profile, IProgressMonitor monitor) throws ProvisionException {
		Assert.isNotNull(profile);
		Assert.isNotNull(toBecome);
		return getPlanner().getBecomePlan(toBecome, profile, monitor);
	}

	/*
	 * Get the plan to uninstall the specified IU's
	 */
	public static ProvisioningPlan getUninstallPlan(IInstallableUnit[] toUninstall, Profile profile, IProgressMonitor monitor) throws ProvisionException {
		Assert.isNotNull(profile);
		Assert.isNotNull(toUninstall);
		return getPlanner().getUninstallPlan(toUninstall, profile, monitor);
	}

	/*
	 * Get sizing info for the specified IU's
	 */
	public static Sizing getSizeInfo(ProvisioningPlan plan, Profile profile, IProgressMonitor monitor) throws ProvisionException {
		SizingPhaseSet set = new SizingPhaseSet();
		IStatus status = getEngine().perform(profile, set, plan.getOperands(), monitor);
		if (status.isOK())
			return set.getSizing();
		return null;
	}

	public static IStatus performInstall(ProvisioningPlan plan, Profile profile, IInstallableUnit[] installRoots, IProgressMonitor monitor) throws ProvisionException {
		IStatus engineResult = performProvisioningPlan(plan, profile, monitor);
		if (engineResult.isOK()) {
			// mark the roots as such
			for (int i = 0; i < installRoots.length; i++)
				profile.setInstallableUnitProfileProperty(installRoots[i], IInstallableUnitConstants.PROFILE_ROOT_IU, Boolean.toString(true));
		}
		return engineResult;
	}

	public static IStatus performProvisioningPlan(ProvisioningPlan plan, Profile profile, IProgressMonitor monitor) throws ProvisionException {
		return getEngine().perform(profile, new DefaultPhaseSet(), plan.getOperands(), monitor);
	}

	private static IMetadataRepository getMetadataRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		return manager.getRepository(location);
	}

	private static Engine getEngine() throws ProvisionException {
		Engine engine = (Engine) ServiceHelper.getService(ProvUIActivator.getContext(), Engine.class.getName());
		if (engine == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoEngineFound);
		}
		return engine;
	}

	private static IPlanner getPlanner() throws ProvisionException {
		IPlanner planner = (IPlanner) ServiceHelper.getService(ProvUIActivator.getContext(), IPlanner.class.getName());
		if (planner == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoPlannerFound);
		}
		return planner;
	}
}
