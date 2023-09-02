/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.console;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.director.PlanExecutionHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.service.environment.EnvironmentInfo;

public class ProvisioningHelper {

	static IRepository<IInstallableUnit> addMetadataRepository(IProvisioningAgent agent, URI location) {
		return addRepository(IMetadataRepositoryManager.class, agent, location, "metadata", //$NON-NLS-1$
				IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
	}

	static IRepository<IInstallableUnit> getMetadataRepository(IProvisioningAgent agent, URI location) {
		return getRepository(IMetadataRepositoryManager.class, agent, location);
	}

	static void removeMetadataRepository(IProvisioningAgent agent, URI location) {
		removeRepository(IMetadataRepositoryManager.class, agent, location);
	}

	static IRepository<IArtifactKey> addArtifactRepository(IProvisioningAgent agent, URI location) {
		return addRepository(IArtifactRepositoryManager.class, agent, location, "artifacts", //$NON-NLS-1$
				IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY);
	}

	static void removeArtifactRepository(IProvisioningAgent agent, URI location) {
		removeRepository(IArtifactRepositoryManager.class, agent, location);
	}

	static IProfile addProfile(IProvisioningAgent agent, String profileId, Map<String, String> properties) throws ProvisionException {
		IProfileRegistry profileRegistry = agent.getService(IProfileRegistry.class);
		if (profileRegistry == null)
			return null;
		IProfile profile = profileRegistry.getProfile(profileId);
		if (profile != null)
			return profile;

		Map<String, String> profileProperties = new HashMap<>(properties);
		if (profileProperties.get(IProfile.PROP_ENVIRONMENTS) == null) {
			EnvironmentInfo info = ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class);
			if (info != null)
				profileProperties.put(IProfile.PROP_ENVIRONMENTS, "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch()); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			else
				profileProperties.put(IProfile.PROP_ENVIRONMENTS, ""); //$NON-NLS-1$
		}

		return profileRegistry.addProfile(profileId, profileProperties);
	}

	static void removeProfile(IProvisioningAgent agent, String profileId) {
		IProfileRegistry profileRegistry = agent.getService(IProfileRegistry.class);
		if (profileRegistry == null)
			return;
		profileRegistry.removeProfile(profileId);
	}

	static IProfile[] getProfiles(IProvisioningAgent agent) {
		IProfileRegistry profileRegistry = agent.getService(IProfileRegistry.class);
		if (profileRegistry == null)
			return new IProfile[0];
		return profileRegistry.getProfiles();
	}

	static IProfile getProfile(IProvisioningAgent agent, String id) {
		IProfileRegistry profileRegistry = agent.getService(IProfileRegistry.class);
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
	static IQueryResult<IInstallableUnit> getInstallableUnits(IProvisioningAgent agent, URI location, IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		IQueryable<IInstallableUnit> queryable = null;
		if (location == null) {
			queryable = agent.getService(IMetadataRepositoryManager.class);
		} else {
			queryable = getMetadataRepository(agent, location);
		}
		if (queryable != null)
			return queryable.query(query, monitor);
		return Collector.emptyCollector();
	}

	static List<URI> getMetadataRepositories(IProvisioningAgent agent) {
		return getRepositories(IArtifactRepositoryManager.class, agent);
	}

	/**
	 * Install the described IU
	 */
	static IStatus install(IProvisioningAgent agent, String unitId, String version, IProfile profile, IProgressMonitor progress) throws ProvisionException {
		if (profile == null)
			return null;
		IQueryResult<IInstallableUnit> units = getInstallableUnits(agent, (URI) null, QueryUtil.createIUQuery(unitId, Version.create(version)), progress);
		if (units.isEmpty()) {
			StringBuilder error = new StringBuilder();
			error.append("Installable unit not found: " + unitId + ' ' + version + '\n'); //$NON-NLS-1$
			error.append("Repositories searched:\n");//$NON-NLS-1$
			for (URI repo : getMetadataRepositories(agent))
				error.append(repo + "\n");//$NON-NLS-1$
			throw new ProvisionException(error.toString());
		}

		IPlanner planner = agent.getService(IPlanner.class);
		if (planner == null)
			throw new ProvisionException("No planner service found.");//$NON-NLS-1$

		IEngine engine = agent.getService(IEngine.class);
		if (engine == null)
			throw new ProvisionException("No director service found."); //$NON-NLS-1$
		ProvisioningContext context = new ProvisioningContext(agent);
		IProfileChangeRequest request = planner.createChangeRequest(profile);
		request.addAll(units.toUnmodifiableSet());
		IProvisioningPlan result = planner.getProvisioningPlan(request, context, progress);
		return PlanExecutionHelper.executePlan(result, engine, context, progress);
	}

	static List<URI> getArtifactRepositories(IProvisioningAgent agent) {
		return getRepositories(IArtifactRepositoryManager.class, agent);
	}

	static IArtifactRepository getArtifactRepository(IProvisioningAgent agent, URI repoURL) {
		return (IArtifactRepository) getRepository(IArtifactRepositoryManager.class, agent, repoURL);
	}

	static long[] getProfileTimestamps(IProvisioningAgent agent, String profileId) {
		if (profileId == null) {
			profileId = IProfileRegistry.SELF;
		}
		IProfileRegistry profileRegistry = agent.getService(IProfileRegistry.class);
		if (profileRegistry == null)
			return null;
		return profileRegistry.listProfileTimestamps(profileId);
	}

	static IStatus revertToPreviousState(IProvisioningAgent agent, IProfile profile, long revertToPreviousState) throws ProvisionException {
		IEngine engine = agent.getService(IEngine.class);
		if (engine == null)
			throw new ProvisionException("No p2 engine found."); //$NON-NLS-1$
		IPlanner planner = agent.getService(IPlanner.class);
		if (planner == null)
			throw new ProvisionException("No planner found."); //$NON-NLS-1$
		IProfileRegistry profileRegistry = agent.getService(IProfileRegistry.class);
		if (profileRegistry == null)
			throw new ProvisionException("profile registry cannot be null"); //$NON-NLS-1$
		// If given profile is null, then get/use the self profile
		if (profile == null) {
			profile = getProfile(agent, IProfileRegistry.SELF);
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
			throw new ProvisionException("target profile with timestamp=" + revertToPreviousState + " not found"); //$NON-NLS-1$//$NON-NLS-2$
		URI[] artifactRepos = getArtifactRepositories(agent).toArray(URI[]::new);
		URI[] metadataRepos = getMetadataRepositories(agent).toArray(URI[]::new);
		IProvisioningPlan plan = planner.getDiffPlan(profile, targetProfile, new NullProgressMonitor());
		ProvisioningContext context = new ProvisioningContext(agent);
		context.setMetadataRepositories(metadataRepos);
		context.setArtifactRepositories(artifactRepos);
		return PlanExecutionHelper.executePlan(plan, engine, context, new NullProgressMonitor());
	}

	/**
	 * Install the described IU
	 */
	static IStatus uninstall(IProvisioningAgent agent, String unitId, String version, IProfile profile, IProgressMonitor progress) throws ProvisionException {
		if (profile == null)
			return null;
		IQueryResult<IInstallableUnit> units = profile.query(QueryUtil.createIUQuery(unitId, Version.create(version)), progress);
		if (units.isEmpty()) {
			StringBuilder error = new StringBuilder();
			error.append("Installable unit not found: " + unitId + ' ' + version + '\n'); //$NON-NLS-1$
			error.append("Repositories searched:\n"); //$NON-NLS-1$
			for (URI repo : getMetadataRepositories(agent)) {
				error.append(repo + "\n"); //$NON-NLS-1$
			}
			throw new ProvisionException(error.toString());
		}

		IPlanner planner = agent.getService(IPlanner.class);
		if (planner == null)
			throw new ProvisionException("No planner service found."); //$NON-NLS-1$

		IEngine engine = agent.getService(IEngine.class);
		if (engine == null)
			throw new ProvisionException("No engine service found."); //$NON-NLS-1$
		ProvisioningContext context = new ProvisioningContext(agent);
		IProfileChangeRequest request = planner.createChangeRequest(profile);
		request.removeAll(units.toUnmodifiableSet());
		IProvisioningPlan result = planner.getProvisioningPlan(request, context, progress);
		return PlanExecutionHelper.executePlan(result, engine, context, progress);
	}

	private static <T> IRepository<T> addRepository(Class<? extends IRepositoryManager<T>> repositoryManager,
			IProvisioningAgent agent, URI location, String nameSuffix, String repoType) {
		IRepositoryManager<T> manager = getRepositoryManager(agent, repositoryManager);
		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			// fall through and create a new repository
			boolean createRepo = "file".equals(location.getScheme()); //$NON-NLS-1$
			if (!createRepo) {
				return null;
			}
		}
		// could not load a repo at that location so create one as a convenience
		String repositoryName = location + " - " + nameSuffix; //$NON-NLS-1$
		try {
			return manager.createRepository(location, repositoryName, repoType, null);
		} catch (ProvisionException e) {
			return null;
		}
	}

	private static <T> void removeRepository(Class<? extends IRepositoryManager<T>> repositoryManager,
			IProvisioningAgent agent, URI location) {
		IRepositoryManager<T> manager = getRepositoryManager(agent, repositoryManager);
		manager.removeRepository(location);
	}

	private static <T> IRepository<T> getRepository(Class<? extends IRepositoryManager<T>> repositoryManager,
			IProvisioningAgent agent, URI location) {
		IRepositoryManager<T> manager = getRepositoryManager(agent, repositoryManager);
		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			// for console, just ignore repositories that can't be read
			return null;
		}
	}

	private static <T> List<URI> getRepositories(Class<? extends IRepositoryManager<T>> repositoryManager,
			IProvisioningAgent agent) {
		IRepositoryManager<T> manager = getRepositoryManager(agent, repositoryManager);
		URI[] repos = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		return Arrays.asList(repos);
	}

	private static <T> IRepositoryManager<T> getRepositoryManager(IProvisioningAgent agent,
			Class<? extends IRepositoryManager<T>> repositoryManager) {
		IRepositoryManager<T> manager = agent.getService(repositoryManager);
		if (manager == null) {
			throw new IllegalStateException("No repository manager found for type " + repositoryManager); //$NON-NLS-1$
		}
		return manager;
	}
}
