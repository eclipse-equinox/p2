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

package org.eclipse.equinox.p2.ui;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.configurator.Configurator;
import org.eclipse.equinox.internal.p2.ui.ApplyProfileChangesDialog;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.repository.IRepositoryInfo;
import org.eclipse.equinox.p2.core.repository.IWritableRepositoryInfo;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.director.Oracle;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.query.CompoundIterator;
import org.eclipse.equinox.p2.query.Query;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

/**
 * Utility methods for clients using the provisioning UI
 * 
 * @since 3.4
 */
public class ProvisioningUtil {
	public static IMetadataRepository addMetadataRepository(URL location, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		IMetadataRepository repo = null;
		repo = manager.loadRepository(location, monitor);
		if (repo == null) {
			throw new ProvisionException(NLS.bind(ProvUIMessages.ProvisioningUtil_AddRepositoryFailure, location.toExternalForm()));
		}
		PropertyChangeEvent event = new PropertyChangeEvent(repo, IProvisioningProperties.REPO_ADDED, null, null);
		ProvUIActivator.getDefault().notifyListeners(event);
		return repo;
	}

	public static IMetadataRepository getMetadataRepository(URL location, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		return manager.getRepository(location);
	}

	public static void removeMetadataRepository(URL location, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		IMetadataRepository repo = manager.getRepository(location);
		if (repo != null)
			manager.removeRepository(repo);
		PropertyChangeEvent event = new PropertyChangeEvent(repo, IProvisioningProperties.REPO_REMOVED, null, null);
		ProvUIActivator.getDefault().notifyListeners(event);

	}

	public static IArtifactRepository addArtifactRepository(URL location, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		// TODO need to get rid of this string constant. see bug #196862
		String repositoryName = location + " - artifacts"; //$NON-NLS-1$
		IArtifactRepository repository = manager.createRepository(location, repositoryName, "org.eclipse.equinox.p2.artifact.repository.simpleRepository"); //$NON-NLS-1$
		if (repository == null) {
			throw new ProvisionException(NLS.bind(ProvUIMessages.ProvisioningUtil_AddRepositoryFailure, location));
		}
		PropertyChangeEvent event = new PropertyChangeEvent(repository, IProvisioningProperties.REPO_ADDED, null, null);
		ProvUIActivator.getDefault().notifyListeners(event);

		return repository;
	}

	public static void removeArtifactRepository(URL location, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		IArtifactRepository[] repos = manager.getKnownRepositories();
		for (int i = 0; i < repos.length; i++) {
			IArtifactRepository repo = repos[i];
			if (repo.getLocation().equals(location)) {
				manager.removeRepository(repo);
				PropertyChangeEvent event = new PropertyChangeEvent(repo, IProvisioningProperties.REPO_REMOVED, null, null);
				ProvUIActivator.getDefault().notifyListeners(event);

				return;
			}
		}
	}

	public static IArtifactRepository[] getArtifactRepositories(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		IArtifactRepository[] repos = manager.getKnownRepositories();
		if (repos != null) {
			return repos;
		}
		return new IArtifactRepository[0];
	}

	public static IArtifactRepository getArtifactRepository(URL repoURL, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IArtifactRepository[] repositories = getArtifactRepositories(monitor, uiInfo);
		if (repositories == null)
			return null;
		for (int i = 0; i < repositories.length; i++) {
			if (repoURL.equals(repositories[i].getLocation()))
				return repositories[i];
		}
		return null;
	}

	public static void addProfile(Profile profile, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		profileRegistry.addProfile(profile);
	}

	public static void removeProfile(String profileId, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound);
		}
		Profile profile = profileRegistry.getProfile(profileId);
		if (profile != null)
			profileRegistry.removeProfile(profile);
	}

	public static Profile[] getProfiles(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
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
	public static IInstallableUnit[] getInstallableUnits(URL location, String id, VersionRange range, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IMetadataRepository[] repositories = null;
		if (location == null) {
			repositories = getMetadataRepositories(monitor, uiInfo);
		} else {
			repositories = new IMetadataRepository[] {getMetadataRepository(location, monitor, uiInfo)};
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
	public static IInstallableUnit[] getInstallableUnits(String profileId, String id, VersionRange range, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		Profile[] profiles = null;
		if (profileId == null) {
			profiles = getProfiles(monitor, uiInfo);
		} else {
			profiles = new Profile[] {getProfile(profileId)};
		}
		Iterator i = Query.getIterator(profiles, id, range, null, false);
		return CompoundIterator.asArray(i, monitor);
	}

	public static IMetadataRepository[] getMetadataRepositories(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoRepositoryManager);
		}
		return manager.getKnownRepositories();
	}

	/*
	 * See if the specified IU's can be installed in the profile
	 */
	public static boolean canInstall(IInstallableUnit[] toInstall, Profile profile, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		Assert.isNotNull(profile);
		Assert.isNotNull(toInstall);

		// TODO should I be getting an Oracle from a service helper?
		// Oracle oracle = (Oracle) ServiceHelper.getService(ProvUIActivator.getContext(), Oracle.class.getName());
		Oracle oracle = new Oracle();
		if (oracle == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoOracleFound);
		}
		return oracle.canInstall(toInstall, profile, monitor).equals(Boolean.TRUE);
	}

	/*
	 * See what updates might be available for a single IU.
	 * Useful when checking for updates and letting the user decide
	 * which IU's to update.
	 */
	public static Collection updatesFor(IInstallableUnit toUpdate, IProgressMonitor monitor) throws ProvisionException {
		Assert.isNotNull(toUpdate);

		// TODO should I be getting an Oracle from a service helper?
		// Oracle oracle = (Oracle) ServiceHelper.getService(ProvUIActivator.getContext(), Oracle.class.getName());
		Oracle oracle = new Oracle();
		if (oracle == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoOracleFound);
		}
		return oracle.hasUpdate(toUpdate);
	}

	/*
	 * See what updates might be available for the specified IU's.
	 * Useful for bulk update that can be directly passed to the update helper.
	 */
	public static IInstallableUnit[] updatesFor(IInstallableUnit[] toUpdate, Profile profile, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		Assert.isNotNull(profile);
		Assert.isNotNull(toUpdate);

		// TODO should I be getting an Oracle from a service helper?
		// Oracle oracle = (Oracle) ServiceHelper.getService(ProvUIActivator.getContext(), Oracle.class.getName());
		Oracle oracle = new Oracle();
		if (oracle == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoOracleFound);
		}
		ArrayList allUpdates = new ArrayList();
		for (int i = 0; i < toUpdate.length; i++) {
			allUpdates.addAll(oracle.hasUpdate(toUpdate[i]));
		}
		return (IInstallableUnit[]) allUpdates.toArray(new IInstallableUnit[allUpdates.size()]);
	}

	/*
	 * Install the specified IU's
	 */
	public static IStatus install(IInstallableUnit[] toInstall, String entryPointName, Profile profile, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		Assert.isNotNull(profile);
		Assert.isNotNull(toInstall);

		IDirector director = (IDirector) ServiceHelper.getService(ProvUIActivator.getContext(), IDirector.class.getName());
		if (director == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoDirectorFound);
		}
		return director.install(toInstall, profile, entryPointName, monitor);
	}

	public static IStatus become(IInstallableUnit toBecome, Profile profile, IProgressMonitor monitor) throws ProvisionException {
		Assert.isNotNull(profile);
		Assert.isNotNull(toBecome);

		IDirector director = (IDirector) ServiceHelper.getService(ProvUIActivator.getContext(), IDirector.class.getName());
		if (director == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoDirectorFound);
		}
		return director.become(toBecome, profile, monitor);
	}

	/*
	 * Uninstall the specified IU's
	 */
	public static IStatus uninstall(IInstallableUnit[] toUninstall, Profile profile, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		Assert.isNotNull(profile);
		Assert.isNotNull(toUninstall);

		IDirector director = (IDirector) ServiceHelper.getService(ProvUIActivator.getContext(), IDirector.class.getName());
		if (director == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoDirectorFound);
		}
		return director.uninstall(toUninstall, profile, monitor);
	}

	/*
	 * Uninstall the specified IU's
	 */
	public static IStatus update(IInstallableUnit[] toUninstall, IInstallableUnit[] replacements, Profile profile, IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		Assert.isNotNull(profile);
		Assert.isNotNull(toUninstall);
		Assert.isNotNull(replacements);

		IDirector director = (IDirector) ServiceHelper.getService(ProvUIActivator.getContext(), IDirector.class.getName());
		if (director == null) {
			throw new ProvisionException(ProvUIMessages.ProvisioningUtil_NoDirectorFound);
		}
		return director.replace(toUninstall, replacements, profile, monitor);
	}

	// TODO This method is only in the util class so that I can generate an
	// event. If the setName API generated this event, callers could just do
	// it directly (and I could make this class/package truly internal....)
	public static IStatus setRepositoryName(IRepositoryInfo repository, String name) {
		IWritableRepositoryInfo writableInfo = (IWritableRepositoryInfo) repository.getAdapter(IWritableRepositoryInfo.class);
		if (writableInfo != null) {
			writableInfo.setName(name);
			PropertyChangeEvent event = new PropertyChangeEvent(repository, IProvisioningProperties.REPO_NAME, null, name);
			ProvUIActivator.getDefault().notifyListeners(event);
			return Status.OK_STATUS;
		}
		return error(ProvUIMessages.ProvisioningUtil_RepoNotWritable);
	}

	public static void requestRestart(boolean restartRequired, IAdaptable uiInfo) {
		int retCode = ApplyProfileChangesDialog.promptForRestart(ProvUI.getShell(uiInfo), restartRequired);
		if (retCode == ApplyProfileChangesDialog.PROFILE_APPLYCHANGES) {
			Configurator configurator = (Configurator) ServiceHelper.getService(ProvUIActivator.getContext(), Configurator.class.getName());
			try {
				configurator.applyConfiguration();
			} catch (IOException e) {
				ProvUI.handleException(e, null);
			}
		} else if (retCode == ApplyProfileChangesDialog.PROFILE_RESTART) {
			PlatformUI.getWorkbench().restart();
		}
	}

	private static IStatus error(String message) {
		return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, message);
	}
}
