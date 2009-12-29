/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.console;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.service.environment.EnvironmentInfo;

public class ProvisioningHelper {

	public static IMetadataRepository addMetadataRepository(URI location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found");
		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}

		// for convenience create and add a repository here
		String repositoryName = location + " - metadata";
		try {
			return manager.createRepository(location, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			return null;
		}
	}

	public static IMetadataRepository getMetadataRepository(URI location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found");
		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			return null;
		}
	}

	public static void removeMetadataRepository(URI location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found");
		manager.removeRepository(location);
	}

	public static IArtifactRepository addArtifactRepository(URI location) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.SERVICE_NAME);
		if (manager == null)
			// TODO log here
			return null;
		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		// could not load a repo at that location so create one as a convenience
		String repositoryName = location + " - artifacts";
		try {
			return manager.createRepository(location, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			return null;
		}
	}

	public static void removeArtifactRepository(URI location) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.SERVICE_NAME);
		if (manager == null)
			// TODO log here
			return;
		manager.removeRepository(location);
	}

	public static IProfile addProfile(String profileId, Properties properties) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.SERVICE_NAME);
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
				profileProperties.put(IProfile.PROP_ENVIRONMENTS, "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch()); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			else
				profileProperties.put(IProfile.PROP_ENVIRONMENTS, "");
		}

		return profileRegistry.addProfile(profileId, profileProperties);
	}

	public static void removeProfile(String profileId) {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.SERVICE_NAME);
		if (profileRegistry == null)
			return;
		profileRegistry.removeProfile(profileId);
	}

	public static IProfile[] getProfiles() {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.SERVICE_NAME);
		if (profileRegistry == null)
			return new IProfile[0];
		return profileRegistry.getProfiles();
	}

	public static IProfile getProfile(String id) {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.SERVICE_NAME);
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
	public static IQueryResult<IInstallableUnit> getInstallableUnits(URI location, IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		IQueryable<IInstallableUnit> queryable = null;
		if (location == null) {
			queryable = (IQueryable<IInstallableUnit>) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
		} else {
			queryable = getMetadataRepository(location);
		}
		if (queryable != null)
			return queryable.query(query, monitor);
		return Collector.emptyCollector();
	}

	public static URI[] getMetadataRepositories() {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
		if (manager == null)
			// TODO log here
			return null;
		URI[] repos = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
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
		IQueryResult<IInstallableUnit> units = getInstallableUnits((URI) null, new InstallableUnitQuery(unitId, Version.create(version)), progress);
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

		IPlanner planner = (IPlanner) ServiceHelper.getService(Activator.getContext(), IPlanner.SERVICE_NAME);
		if (planner == null)
			throw new ProvisionException("No planner service found.");

		IEngine engine = (IEngine) ServiceHelper.getService(Activator.getContext(), IEngine.SERVICE_NAME);
		if (engine == null)
			throw new ProvisionException("No director service found.");
		ProvisioningContext context = new ProvisioningContext();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(units);
		IProvisioningPlan result = planner.getProvisioningPlan(request, context, progress);
		return PlanExecutionHelper.executePlan(result, engine, context, progress);
	}

	public static URI[] getArtifactRepositories() {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.SERVICE_NAME);
		if (manager == null)
			// TODO log here
			return null;
		URI[] repos = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		if (repos.length > 0)
			return repos;
		return null;
	}

	public static IArtifactRepository getArtifactRepository(URI repoURL) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.SERVICE_NAME);
		try {
			if (manager != null)
				return manager.loadRepository(repoURL, null);
		} catch (ProvisionException e) {
			//for console, just ignore repositories that can't be read
		}
		return null;
	}

	public static long[] getProfileTimestamps(String profileId) {
		if (profileId == null) {
			profileId = IProfileRegistry.SELF;
		}
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.SERVICE_NAME);
		if (profileRegistry == null)
			return null;
		return profileRegistry.listProfileTimestamps(profileId);
	}

	public static IStatus revertToPreviousState(IProfile profile, long revertToPreviousState) throws ProvisionException {
		IEngine engine = (IEngine) ServiceHelper.getService(Activator.getContext(), IEngine.SERVICE_NAME);
		if (engine == null)
			throw new ProvisionException("No p2 engine found.");
		IPlanner planner = (IPlanner) ServiceHelper.getService(Activator.getContext(), IPlanner.SERVICE_NAME);
		if (planner == null)
			throw new ProvisionException("No planner found.");
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.SERVICE_NAME);
		if (profileRegistry == null)
			throw new ProvisionException("profile registry cannot be null");
		// If given profile is null, then get/use the self profile
		if (profile == null) {
			profile = getProfile(IProfileRegistry.SELF);
		}
		IProfile targetProfile = null;
		if (revertToPreviousState == 0) {
			long[] profiles = profileRegistry.listProfileTimestamps(profile.getProfileId());
			if (profiles.length == 0)
				// Nothing to do, as the profile does not have any previous timestamps
				return Status.OK_STATUS;
			targetProfile = profileRegistry.getProfile(profile.getProfileId(), profiles[profiles.length - 1]);
		} else {
			targetProfile = profileRegistry.getProfile(profile.getProfileId(), revertToPreviousState);
		}
		if (targetProfile == null)
			throw new ProvisionException("target profile with timestamp=" + revertToPreviousState + " not found");
		URI[] artifactRepos = getArtifactRepositories();
		URI[] metadataRepos = getMetadataRepositories();
		IProvisioningPlan plan = planner.getDiffPlan(profile, targetProfile, new NullProgressMonitor());
		ProvisioningContext context = new ProvisioningContext(metadataRepos);
		context.setArtifactRepositories(artifactRepos);
		return PlanExecutionHelper.executePlan(plan, engine, context, new NullProgressMonitor());
	}

	/**
	 * Install the described IU
	 */
	public static IStatus uninstall(String unitId, String version, IProfile profile, IProgressMonitor progress) throws ProvisionException {
		if (profile == null)
			return null;
		IQueryResult units = profile.query(new InstallableUnitQuery(unitId, Version.create(version)), progress);
		if (units.isEmpty()) {
			StringBuffer error = new StringBuffer();
			error.append("Installable unit not found: " + unitId + ' ' + version + '\n');
			error.append("Repositories searched:\n");
			URI[] repos = getMetadataRepositories();
			if (repos != null) {
				for (int i = 0; i < repos.length; i++)
					error.append(repos[i] + "\n"); //$NON-NLS-1$
			}
			throw new ProvisionException(error.toString());
		}

		IPlanner planner = (IPlanner) ServiceHelper.getService(Activator.getContext(), IPlanner.SERVICE_NAME);
		if (planner == null)
			throw new ProvisionException("No planner service found.");

		IEngine engine = (IEngine) ServiceHelper.getService(Activator.getContext(), IEngine.SERVICE_NAME);
		if (engine == null)
			throw new ProvisionException("No engine service found.");
		ProvisioningContext context = new ProvisioningContext();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.removeInstallableUnits(units);
		IProvisioningPlan result = planner.getProvisioningPlan(request, context, progress);
		return PlanExecutionHelper.executePlan(result, engine, context, progress);
	}

}
