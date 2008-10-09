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
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.*;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.Version;

public class ProvisioningHelper {

	public static IMetadataRepository addMetadataRepository(URI location) {
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
			return manager.createRepository(location, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			return null;
		}
	}

	public static IMetadataRepository getMetadataRepository(URI location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found");
		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			return null;
		}
	}

	public static void removeMetadataRepository(URI location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found");
		manager.removeRepository(location);
	}

	public static IArtifactRepository addArtifactRepository(URI location) {
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
			return manager.createRepository(location, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			return null;
		}
	}

	public static void removeArtifactRepository(URI location) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			// TODO log here
			return;
		manager.removeRepository(location);
	}

	public static IProfile addProfile(String profileId, Properties properties) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null)
			return null;
		IProfile profile = profileRegistry.getProfile(profileId);
		if (profile != null)
			return profile;

		Map profileProperties = new HashMap();

		for (Iterator it = properties.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			profileProperties.put(key, properties.getProperty(key));
		}

		if (profileProperties.get(IProfile.PROP_ENVIRONMENTS) == null) {
			EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
			if (info != null)
				profileProperties.put(IProfile.PROP_ENVIRONMENTS, "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch());
			else
				profileProperties.put(IProfile.PROP_ENVIRONMENTS, "");
		}

		return profileRegistry.addProfile(profileId, profileProperties);
	}

	public static void removeProfile(String profileId) {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null)
			return;
		profileRegistry.removeProfile(profileId);
	}

	public static IProfile[] getProfiles() {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null)
			return new IProfile[0];
		return profileRegistry.getProfiles();
	}

	public static IProfile getProfile(String id) {
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
	public static Collector getInstallableUnits(URI location, Query query, IProgressMonitor monitor) {
		return getInstallableUnits(location, query, new Collector(), monitor);
	}

	public static Collector getInstallableUnits(URI location, Query query, Collector collector, IProgressMonitor monitor) {
		IQueryable queryable = null;
		if (location == null) {
			queryable = (IQueryable) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		} else {
			queryable = getMetadataRepository(location);
		}
		return queryable.query(query, collector, monitor);
	}

	public static URI[] getMetadataRepositories() {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			// TODO log here
			return null;
		URI[] repos = manager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL);
		if (repos.length > 0)
			return repos;
		return null;
	}

	/**
	 * Install the described IU
	 */
	public static IStatus install(String unitId, String version, IProfile profile, IProgressMonitor progress) throws ProvisionException {
		if (profile == null)
			return null;
		Collector units = getInstallableUnits(null, new InstallableUnitQuery(unitId, new Version(version)), progress);
		if (units.isEmpty()) {
			StringBuffer error = new StringBuffer();
			error.append("Installable unit not found: " + unitId + ' ' + version + '\n');
			error.append("Repositories searched:\n");
			URI[] repos = getMetadataRepositories();
			if (repos != null) {
				for (int i = 0; i < repos.length; i++)
					error.append(repos[i] + "\n");
			}
			throw new ProvisionException(error.toString());
		}

		IPlanner planner = (IPlanner) ServiceHelper.getService(Activator.getContext(), IPlanner.class.getName());
		if (planner == null)
			throw new ProvisionException("No planner service found.");

		IEngine engine = (IEngine) ServiceHelper.getService(Activator.getContext(), IEngine.SERVICE_NAME);
		if (engine == null)
			throw new ProvisionException("No director service found.");
		IInstallableUnit[] toInstall = (IInstallableUnit[]) units.toArray(IInstallableUnit.class);
		ProvisioningContext context = new ProvisioningContext();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(toInstall);
		ProvisioningPlan result = planner.getProvisioningPlan(request, context, progress);
		if (!result.getStatus().isOK())
			return result.getStatus();

		return engine.perform(profile, new DefaultPhaseSet(), result.getOperands(), context, progress);
	}

	/**
	 * Uninstall the described IU
	 */
	public static IStatus uninstall(String unitId, String version, IProfile profile, IProgressMonitor progress) throws ProvisionException {
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

	public static URI[] getArtifactRepositories() {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			// TODO log here
			return null;
		URI[] repos = manager.getKnownRepositories(IArtifactRepositoryManager.REPOSITORIES_ALL);
		if (repos.length > 0)
			return repos;
		return null;
	}

	public static IArtifactRepository getArtifactRepository(URI repoURL) {
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
