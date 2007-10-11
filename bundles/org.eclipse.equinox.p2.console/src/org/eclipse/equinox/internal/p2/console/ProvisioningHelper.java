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
package org.eclipse.equinox.internal.p2.console;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.configurator.Configurator;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.director.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.query.CompoundIterator;
import org.eclipse.equinox.p2.query.Query;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class ProvisioningHelper {

	public static IMetadataRepository addMetadataRepository(URL location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found");
		IMetadataRepository repository = manager.loadRepository(location, null);
		if (repository != null)
			return repository;

		// for convenience create and add a repo here
		// TODO need to get rid o fthe factory method.
		String repositoryName = location + " - metadata"; //$NON-NLS-1$
		IMetadataRepository result = manager.createRepository(location, repositoryName, "org.eclipse.equinox.p2.metadata.repository.simpleRepository"); //$NON-NLS-1$
		return result;
	}

	public static IMetadataRepository getMetadataRepository(URL location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found");
		return manager.getRepository(location);
	}

	public static void removeMetadataRepository(URL location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found");
		IMetadataRepository repo = manager.getRepository(location);
		if (repo != null)
			manager.removeRepository(repo);
	}

	public static IArtifactRepository addArtifactRepository(URL location) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			// TODO log here
			return null;
		IArtifactRepository repository = manager.loadRepository(location, null);
		if (repository != null)
			return repository;

		// could not load a repo at that location so create one as a convenience
		String repositoryName = location + " - artifacts"; //$NON-NLS-1$
		return manager.createRepository(location, repositoryName, "org.eclipse.equinox.p2.artifact.repository.simpleRepository"); //$NON-NLS-1$
	}

	public static void removeArtifactRepository(URL location) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			// TODO log here
			return;
		IArtifactRepository[] repos = manager.getKnownRepositories();
		for (int i = 0; i < repos.length; i++) {
			IArtifactRepository repo = repos[i];
			if (repo.getLocation().equals(location)) {
				manager.removeRepository(repo);
				return;
			}
		}
	}

	public static Profile addProfile(String profileId, Properties properties) {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null)
			return null;
		Profile profile = profileRegistry.getProfile(profileId);
		if (profile != null)
			return profile;

		profile = new Profile(profileId);

		for (Iterator it = properties.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			profile.setValue(key, properties.getProperty(key));
		}

		if (profile.getValue(Profile.PROP_ENVIRONMENTS) == null) {
			EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
			if (info != null)
				profile.setValue(Profile.PROP_ENVIRONMENTS, "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch());
			else
				profile.setValue(Profile.PROP_ENVIRONMENTS, "");
		}

		profileRegistry.addProfile(profile);
		return profile;
	}

	public static void removeProfile(String profileId) {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null)
			return;
		Profile profile = profileRegistry.getProfile(profileId);
		if (profile != null)
			profileRegistry.removeProfile(profile);
	}

	public static Profile[] getProfiles() {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null)
			return new Profile[0];
		return profileRegistry.getProfiles();
	}

	public static Profile getProfile(String id) {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null)
			return null;
		return profileRegistry.getProfile(id);
	}

	/**
	 * Returns the installable units with the given id and version
	 * specifications in the given metadata repository.  <code>null</code>
	 * can be used to indicate wildcards for any of the arguments.
	 * 
	 * @param location The location of the metdata repo to search.  <code>null</code> indicates
	 *        search all known repos.
	 * @param id The id of the IUs to find. <code>null</code> indicates
	 *        wildcard.
	 * @param range The version range of the IUs to find. <code>null</code>
	 *        indicates wildcard.
	 * @return The IUs that match the query
	 */
	public static IInstallableUnit[] getInstallableUnits(URL location, String id, VersionRange range, IProgressMonitor progress) {
		IMetadataRepository[] repositories = null;
		if (location == null)
			repositories = getMetadataRepositories();
		else
			repositories = new IMetadataRepository[] {getMetadataRepository(location)};
		Iterator i = Query.getIterator(repositories, id, range, null, false);
		return CompoundIterator.asArray(i, progress);
	}

	/**
	 * Returns the installable units with the given id and version
	 * specifications.
	 * 
	 * @param profileId The profile to search
	 * @param id The id of the IUs to find. <code>null</code> indicates
	 *        wildcard.
	 * @param range The version range of the IUs to find. <code>null</code>
	 *        indicates wildcard.
	 * @return The IUs that match the query
	 */
	public static IInstallableUnit[] getInstallableUnits(String profileId, String id, VersionRange range, IProgressMonitor progress) {
		Profile[] profiles = null;
		if (profileId == null)
			profiles = getProfiles();
		else
			profiles = new Profile[] {getProfile(profileId)};
		Iterator i = Query.getIterator(profiles, id, range, null, false);
		return CompoundIterator.asArray(i, progress);
	}

	public static IMetadataRepository[] getMetadataRepositories() {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			// TODO log here
			return null;
		IMetadataRepository[] repos = manager.getKnownRepositories();
		if (repos.length > 0)
			return repos;
		return null;
	}

	/**
	 * Install the described IU
	 */
	public static IStatus install(String unitId, String version, Profile profile, IProgressMonitor progress) throws ProvisionException {
		IMetadataRepository[] repos = getMetadataRepositories();
		if (repos == null || profile == null)
			return null;
		// search for a matching IU in the known repositories
		IInstallableUnit toInstall = null;
		Version unitVersion = new Version(version);
		outer: for (int i = 0; i < repos.length; i++) {
			IInstallableUnit[] ius = repos[i].getInstallableUnits(progress);
			for (int j = 0; j < ius.length; j++) {
				if (unitId.equals(ius[j].getId()) && unitVersion.equals(ius[j].getVersion())) {
					toInstall = ius[j];
					break outer;
				}
			}
		}
		if (toInstall == null) {
			StringBuffer error = new StringBuffer();
			error.append("Installable unit not found: " + unitId + ' ' + unitVersion + '\n');
			error.append("Repositories searched:\n");
			for (int i = 0; i < repos.length; i++)
				error.append(repos[i].getLocation() + "\n");
			throw new ProvisionException(error.toString());
		}

		IPlanner planner = (IPlanner) ServiceHelper.getService(Activator.getContext(), IPlanner.class.getName());
		if (planner == null)
			throw new ProvisionException("No planner service found.");

		Engine engine = (Engine) ServiceHelper.getService(Activator.getContext(), Engine.class.getName());
		if (engine == null)
			throw new ProvisionException("No director service found.");
		ProvisioningPlan result = planner.getInstallPlan(new IInstallableUnit[] {toInstall}, profile, progress);
		if (!result.getStatus().isOK())
			return result.getStatus();

		return engine.perform(profile, new DefaultPhaseSet(), result.getOperands(), progress);
	}

	/**
	 * Uninstall the described IU
	 */
	public static IStatus uninstall(String unitId, String version, Profile profile, IProgressMonitor progress) throws ProvisionException {
		IDirector director = (IDirector) ServiceHelper.getService(Activator.getContext(), IDirector.class.getName());
		if (director == null)
			throw new ProvisionException("No director service found.");

		// return director.uninstall(new InstallableUnit[] {toInstall}, profile,
		// null);
		return null;
	}

	public static void kick(String profileId) {
		Configurator configurator = (Configurator) ServiceHelper.getService(Activator.getContext(), Configurator.class.getName());
		if (configurator == null)
			return;
		if (profileId == null)
			try {
				configurator.applyConfiguration();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		else {
			// TODO do some work here to figure out how to kick some random profile			
			//					configurator.applyConfiguration(configURL);
		}
	}

	public static IArtifactRepository[] getArtifactRepositories() {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			// TODO log here
			return null;
		IArtifactRepository[] repos = manager.getKnownRepositories();
		if (repos.length > 0)
			return repos;
		return null;
	}

	public static IArtifactRepository getArtifactRepository(URL repoURL) {
		IArtifactRepository[] repositories = getArtifactRepositories();
		if (repositories == null)
			return null;
		for (int i = 0; i < repositories.length; i++)
			if (repoURL.equals(repositories[i].getLocation()))
				return repositories[i];
		return null;
	}
}
