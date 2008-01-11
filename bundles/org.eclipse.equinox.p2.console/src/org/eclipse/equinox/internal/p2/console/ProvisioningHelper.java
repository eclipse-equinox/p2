/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.Version;

public class ProvisioningHelper {

	public static IMetadataRepository addMetadataRepository(URL location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found"); //$NON-NLS-1$
		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}

		// for convenience create and add a repository here
		String repositoryName = location + " - metadata"; //$NON-NLS-1$
		try {
			return manager.createRepository(location, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
		} catch (ProvisionException e) {
			return null;
		}
	}

	public static IMetadataRepository getMetadataRepository(URL location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found");
		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			return null;
		}
	}

	public static void removeMetadataRepository(URL location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found");
		manager.removeRepository(location);
	}

	public static IArtifactRepository addArtifactRepository(URL location) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			// TODO log here
			return null;
		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		// could not load a repo at that location so create one as a convenience
		String repositoryName = location + " - artifacts"; //$NON-NLS-1$
		try {
			return manager.createRepository(location, repositoryName, "org.eclipse.equinox.p2.artifact.repository.simpleRepository"); //$NON-NLS-1$
		} catch (ProvisionException e) {
			return null;
		}
	}

	public static void removeArtifactRepository(URL location) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			// TODO log here
			return;
		manager.removeRepository(location);
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
	 * Returns the installable units that match the given query
	 * in the given metadata repository.
	 * 
	 * @param location The location of the metadata repo to search.  <code>null</code> indicates
	 *        search all known repos.
	 * @param query The query to perform
	 * @param monitor A progress monitor, or <code>null</code>
	 * @return The IUs that match the query
	 */
	public static Collector getInstallableUnits(URL location, Query query, IProgressMonitor monitor) {
		IQueryable queryable = null;
		if (location == null) {
			queryable = (IQueryable) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		} else {
			queryable = getMetadataRepository(location);
		}
		return queryable.query(query, new Collector(), null);
	}

	public static URL[] getMetadataRepositories() {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			// TODO log here
			return null;
		URL[] repos = manager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL);
		if (repos.length > 0)
			return repos;
		return null;
	}

	/**
	 * Install the described IU
	 */
	public static IStatus install(String unitId, String version, Profile profile, IProgressMonitor progress) throws ProvisionException {
		if (profile == null)
			return null;
		Collector units = getInstallableUnits(null, new InstallableUnitQuery(unitId, new Version(version)), progress);
		if (units.isEmpty()) {
			StringBuffer error = new StringBuffer();
			error.append("Installable unit not found: " + unitId + ' ' + version + '\n');
			error.append("Repositories searched:\n");
			URL[] repos = getMetadataRepositories();
			for (int i = 0; i < repos.length; i++)
				error.append(repos[i] + "\n");
			throw new ProvisionException(error.toString());
		}

		IPlanner planner = (IPlanner) ServiceHelper.getService(Activator.getContext(), IPlanner.class.getName());
		if (planner == null)
			throw new ProvisionException("No planner service found.");

		Engine engine = (Engine) ServiceHelper.getService(Activator.getContext(), Engine.class.getName());
		if (engine == null)
			throw new ProvisionException("No director service found.");
		IInstallableUnit[] toInstall = (IInstallableUnit[]) units.toArray(IInstallableUnit.class);
		ProvisioningPlan result = planner.getInstallPlan(toInstall, profile, null, progress);
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

	public static URL[] getArtifactRepositories() {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			// TODO log here
			return null;
		URL[] repos = manager.getKnownRepositories(IArtifactRepositoryManager.REPOSITORIES_ALL);
		if (repos.length > 0)
			return repos;
		return null;
	}

	public static IArtifactRepository getArtifactRepository(URL repoURL) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		try {
			if (manager != null)
				return manager.loadRepository(repoURL, null);
		} catch (ProvisionException e) {
			//for console, just ignore repositories that can't be read
		}
		return null;
	}
}
