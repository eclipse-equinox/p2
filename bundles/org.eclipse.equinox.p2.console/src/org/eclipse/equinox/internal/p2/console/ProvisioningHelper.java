/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.console;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.director.PlanExecutionHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.service.environment.EnvironmentInfo;

public class ProvisioningHelper {

	static IMetadataRepository addMetadataRepository(IProvisioningAgent agent, URI location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		boolean createRepo = "file".equals(location.getScheme()); //$NON-NLS-1$

		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found"); //$NON-NLS-1$
		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			if (!createRepo)
				return null;
		}

		// for convenience create and add a repository here
		String repositoryName = location + " - metadata"; //$NON-NLS-1$
		try {
			return manager.createRepository(location, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			return null;
		}
	}

	static IMetadataRepository getMetadataRepository(IProvisioningAgent agent, URI location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found"); //$NON-NLS-1$
		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			return null;
		}
	}

	static void removeMetadataRepository(IProvisioningAgent agent, URI location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found"); //$NON-NLS-1$
		manager.removeRepository(location);
	}

	static IArtifactRepository addArtifactRepository(IProvisioningAgent agent, URI location) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		boolean createRepo = "file".equals(location.getScheme()); //$NON-NLS-1$

		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found"); //$NON-NLS-1$

		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			//fall through and create a new repository
			if (!createRepo)
				return null;
		}
		// could not load a repo at that location so create one as a convenience
		String repositoryName = location + " - artifacts"; //$NON-NLS-1$
		try {
			return manager.createRepository(location, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			return null;
		}
	}

	static void removeArtifactRepository(IProvisioningAgent agent, URI location) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		if (manager == null)
			// TODO log here
			return;
		manager.removeRepository(location);
	}

	static IProfile addProfile(IProvisioningAgent agent, String profileId, Map<String, String> properties) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (profileRegistry == null)
			return null;
		IProfile profile = profileRegistry.getProfile(profileId);
		if (profile != null)
			return profile;

		Map<String, String> profileProperties = new HashMap<String, String>(properties);
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
		IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (profileRegistry == null)
			return;
		profileRegistry.removeProfile(profileId);
	}

	static IProfile[] getProfiles(IProvisioningAgent agent) {
		IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (profileRegistry == null)
			return new IProfile[0];
		return profileRegistry.getProfiles();
	}

	static IProfile getProfile(IProvisioningAgent agent, String id) {
		IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
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
			queryable = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		} else {
			queryable = getMetadataRepository(agent, location);
		}
		if (queryable != null)
			return queryable.query(query, monitor);
		return Collector.emptyCollector();
	}

	static URI[] getMetadataRepositories(IProvisioningAgent agent) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
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
	static IStatus install(IProvisioningAgent agent, String unitId, String version, IProfile profile, IProgressMonitor progress) throws ProvisionException {
		if (profile == null)
			return null;
		IQueryResult<IInstallableUnit> units = getInstallableUnits(agent, (URI) null, QueryUtil.createIUQuery(unitId, Version.create(version)), progress);
		if (units.isEmpty()) {
			StringBuffer error = new StringBuffer();
			error.append("Installable unit not found: " + unitId + ' ' + version + '\n'); //$NON-NLS-1$
			error.append("Repositories searched:\n");//$NON-NLS-1$
			URI[] repos = getMetadataRepositories(agent);
			if (repos != null) {
				for (int i = 0; i < repos.length; i++)
					error.append(repos[i] + "\n");//$NON-NLS-1$
			}
			throw new ProvisionException(error.toString());
		}

		IPlanner planner = (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
		if (planner == null)
			throw new ProvisionException("No planner service found.");//$NON-NLS-1$

		IEngine engine = (IEngine) agent.getService(IEngine.SERVICE_NAME);
		if (engine == null)
			throw new ProvisionException("No director service found."); //$NON-NLS-1$
		ProvisioningContext context = new ProvisioningContext(agent);
		IProfileChangeRequest request = planner.createChangeRequest(profile);
		request.addAll(units.toUnmodifiableSet());
		IProvisioningPlan result = planner.getProvisioningPlan(request, context, progress);
		return PlanExecutionHelper.executePlan(result, engine, context, progress);
	}

	static URI[] getArtifactRepositories(IProvisioningAgent agent) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found"); //$NON-NLS-1$
		URI[] repos = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		if (repos.length > 0)
			return repos;
		return null;
	}

	static IArtifactRepository getArtifactRepository(IProvisioningAgent agent, URI repoURL) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		try {
			if (manager != null)
				return manager.loadRepository(repoURL, null);
		} catch (ProvisionException e) {
			//for console, just ignore repositories that can't be read
		}
		return null;
	}

	static long[] getProfileTimestamps(IProvisioningAgent agent, String profileId) {
		if (profileId == null) {
			profileId = IProfileRegistry.SELF;
		}
		IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (profileRegistry == null)
			return null;
		return profileRegistry.listProfileTimestamps(profileId);
	}

	static IStatus revertToPreviousState(IProvisioningAgent agent, IProfile profile, long revertToPreviousState) throws ProvisionException {
		IEngine engine = (IEngine) agent.getService(IEngine.SERVICE_NAME);
		if (engine == null)
			throw new ProvisionException("No p2 engine found."); //$NON-NLS-1$
		IPlanner planner = (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
		if (planner == null)
			throw new ProvisionException("No planner found."); //$NON-NLS-1$
		IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
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
		URI[] artifactRepos = getArtifactRepositories(agent);
		URI[] metadataRepos = getMetadataRepositories(agent);
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
			StringBuffer error = new StringBuffer();
			error.append("Installable unit not found: " + unitId + ' ' + version + '\n'); //$NON-NLS-1$
			error.append("Repositories searched:\n"); //$NON-NLS-1$
			URI[] repos = getMetadataRepositories(agent);
			if (repos != null) {
				for (int i = 0; i < repos.length; i++)
					error.append(repos[i] + "\n"); //$NON-NLS-1$
			}
			throw new ProvisionException(error.toString());
		}

		IPlanner planner = (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
		if (planner == null)
			throw new ProvisionException("No planner service found."); //$NON-NLS-1$

		IEngine engine = (IEngine) agent.getService(IEngine.SERVICE_NAME);
		if (engine == null)
			throw new ProvisionException("No engine service found."); //$NON-NLS-1$
		ProvisioningContext context = new ProvisioningContext(agent);
		IProfileChangeRequest request = planner.createChangeRequest(profile);
		request.removeAll(units.toUnmodifiableSet());
		IProvisioningPlan result = planner.getProvisioningPlan(request, context, progress);
		return PlanExecutionHelper.executePlan(result, engine, context, progress);
	}

}
