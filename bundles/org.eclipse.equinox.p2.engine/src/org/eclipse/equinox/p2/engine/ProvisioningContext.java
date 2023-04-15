/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 * 	IBM Corporation - initial API and implementation
 *     WindRiver - https://bugs.eclipse.org/bugs/show_bug.cgi?id=227372
 *	Sonatype, Inc. - ongoing development
 *  Karsten Thoms - Bug#527874
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.DebugHelper;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 * A provisioning context defines the scope in which a provisioning operation
 * occurs. A context can be used to specify the set of repositories available
 * to the planner and engine as they perform provisioning work.
 * @since 2.0
 */
public class ProvisioningContext {
	private IProvisioningAgent agent;
	private URI[] artifactRepositories; //artifact repositories to consult
	private final List<IInstallableUnit> extraIUs = Collections.synchronizedList(new ArrayList<IInstallableUnit>());
	private URI[] metadataRepositories; //metadata repositories to consult
	private final Map<String, String> properties = new HashMap<>();
	private Map<String, URI> referencedArtifactRepositories = null;
	private Map<URI, IArtifactRepository> loadedArtifactRepositories = new HashMap<>();
	private Map<URI, IMetadataRepository> loadedMetadataRepositories = new HashMap<>();
	private Map<URI, IMetadataRepository> allLoadedMetadataRepositories;
	private Map<URI, IArtifactRepository> allLoadedArtifactRepositories;
	private Set<URI> failedArtifactRepositories = new HashSet<>();
	private Set<URI> failedMetadataRepositories = new HashSet<>();

	private static final String FILE_PROTOCOL = "file"; //$NON-NLS-1$

	class ArtifactRepositoryQueryable implements IQueryable<IArtifactRepository> {
		List<IArtifactRepository> repositories;

		ArtifactRepositoryQueryable(List<IArtifactRepository> repositories) {
			this.repositories = repositories;
		}

		@Override
		public IQueryResult<IArtifactRepository> query(IQuery<IArtifactRepository> query, IProgressMonitor mon) {
			return query.perform(repositories.listIterator());
		}
	}

	/**
	 * This Comparator sorts the repositories such that local repositories are first
	 */
	private static final Comparator<URI> LOCAL_FIRST_COMPARATOR = (arg0, arg1) -> {
		String protocol0 = arg0.getScheme();
		String protocol1 = arg1.getScheme();

		if (FILE_PROTOCOL.equals(protocol0) && !FILE_PROTOCOL.equals(protocol1))
			return -1;
		if (!FILE_PROTOCOL.equals(protocol0) && FILE_PROTOCOL.equals(protocol1))
			return 1;
		return 0;
	};

	/**
	 * Instructs the provisioning context to follow metadata repository references when
	 * providing queryables for obtaining metadata and artifacts.  When this property is set to
	 * "true", then metadata repository references that are encountered while loading the
	 * specified metadata repositories will be included in the provisioning
	 * context.
	 *
	 * @see #getMetadata(IProgressMonitor)
	 * @see #setMetadataRepositories(URI[])
	 */
	public static final String FOLLOW_REPOSITORY_REFERENCES = "org.eclipse.equinox.p2.director.followRepositoryReferences"; //$NON-NLS-1$

	/**
	 * Instructs the {@link PhaseSetFactory#PHASE_COLLECT collect phase} to check
	 * the originating sources of all installable units and artifacts.
	 *
	 * @see #getInstallableUnitSources(Collection, IProgressMonitor)
	 * @see #getArtifactSources(Collection, IProgressMonitor)
	 * @since 2.8
	 */
	public static final String CHECK_AUTHORITIES = "org.eclipse.equinox.p2.director.checkAuthorities"; //$NON-NLS-1$

	private static final String FOLLOW_ARTIFACT_REPOSITORY_REFERENCES = "org.eclipse.equinox.p2.director.followArtifactRepositoryReferences"; //$NON-NLS-1$

	/**
	 * Creates a new provisioning context that includes all available metadata and
	 * artifact repositories available to the specified provisioning agent.
	 *
	 * @param agent the provisioning agent from which to obtain any necessary services.
	 */
	public ProvisioningContext(IProvisioningAgent agent) {
		this.agent = agent;
		// null repos means look at them all
		metadataRepositories = null;
		artifactRepositories = null;
		setProperty(FOLLOW_ARTIFACT_REPOSITORY_REFERENCES, Boolean.TRUE.toString());
	}

	/**
	 * Returns a queryable that can be used to obtain any artifact keys that
	 * are needed for the provisioning operation.
	 *
	 * @param monitor a progress monitor to be used when creating the queryable
	 * @return a queryable that can be used to query available artifact keys.
	 *
	 * @see #setArtifactRepositories(URI[])
	 */
	public IQueryable<IArtifactKey> getArtifactKeys(IProgressMonitor monitor) {
		return QueryUtil.compoundQueryable(getLoadedArtifactRepositories(monitor));
	}

	/**
	 * Returns a queryable that can be used to obtain any artifact descriptors that
	 * are needed for the provisioning operation.
	 *
	 * @param monitor a progress monitor to be used when creating the queryable
	 * @return a queryable that can be used to query available artifact descriptors.
	 *
	 * @see #setArtifactRepositories(URI[])
	 */
	public IQueryable<IArtifactDescriptor> getArtifactDescriptors(IProgressMonitor monitor) {
		List<IArtifactRepository> repos = getLoadedArtifactRepositories(monitor);
		List<IQueryable<IArtifactDescriptor>> descriptorQueryables = new ArrayList<>();
		for (IArtifactRepository repo : repos) {
			descriptorQueryables.add(repo.descriptorQueryable());
		}
		return QueryUtil.compoundQueryable(descriptorQueryables);
	}

	/**
	 * Returns a queryable that can be used to obtain any artifact repositories that
	 * are needed for the provisioning operation.
	 *
	 * @param monitor a progress monitor to be used when creating the queryable
	 * @return a queryable that can be used to query available artifact repositories.
	 *
	 * @see #setArtifactRepositories(URI[])
	 */
	public IQueryable<IArtifactRepository> getArtifactRepositories(IProgressMonitor monitor) {
		return new ArtifactRepositoryQueryable(getLoadedArtifactRepositories(monitor));
	}

	/**
	 * Return an array of loaded artifact repositories.
	 */
	private List<IArtifactRepository> getLoadedArtifactRepositories(IProgressMonitor monitor) {
		IArtifactRepositoryManager repoManager = agent.getService(IArtifactRepositoryManager.class);
		URI[] repositories = artifactRepositories == null ? repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL) : artifactRepositories;
		Arrays.sort(repositories, LOCAL_FIRST_COMPARATOR);

		List<IArtifactRepository> repos = new ArrayList<>();
		SubMonitor sub = SubMonitor.convert(monitor, (repositories.length + 1) * 100);
		for (URI location : repositories) {
			getLoadedRepository(location, repoManager, repos, sub);
			// Remove this URI from the list of extra references if it is there.
			if (referencedArtifactRepositories != null && location != null) {
				referencedArtifactRepositories.remove(location.toString());
			}
		}
		// Are there any extra artifact repository references to consider?
		if (referencedArtifactRepositories != null && referencedArtifactRepositories.size() > 0 && shouldFollowArtifactReferences()) {
			SubMonitor innerSub = SubMonitor.convert(sub.newChild(100), referencedArtifactRepositories.size() * 100);
			for (URI referencedURI : referencedArtifactRepositories.values()) {
				getLoadedRepository(referencedURI, repoManager, repos, innerSub);
			}
		}
		return repos;
	}

	private void getLoadedRepository(URI location, IArtifactRepositoryManager repoManager, List<IArtifactRepository> repos, SubMonitor monitor) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		if (failedArtifactRepositories.contains(location)) {
			return;
		}
		try {
			IArtifactRepository repository = loadedArtifactRepositories.get(location);
			if (repository == null) {
				repository = repoManager.loadRepository(location, monitor.newChild(100));
				loadedArtifactRepositories.put(location, repository);
			}
			repos.add(repository);
		} catch (ProvisionException e) {
			//skip and remember unreadable repositories
			failedArtifactRepositories.add(location);
		}
	}

	private Set<IMetadataRepository> getLoadedMetadataRepositories(IProgressMonitor monitor) {
		IMetadataRepositoryManager repoManager = agent.getService(IMetadataRepositoryManager.class);
		URI[] repositories = metadataRepositories == null ? repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL) : metadataRepositories;

		HashMap<String, IMetadataRepository> repos = new HashMap<>();
		SubMonitor sub = SubMonitor.convert(monitor, repositories.length * 100);

		// Clear out the list of remembered artifact repositories
		referencedArtifactRepositories = new HashMap<>();
		for (URI repositorie : repositories) {
			if (sub.isCanceled())
				throw new OperationCanceledException();
			loadMetadataRepository(repoManager, repositorie, repos, shouldFollowReferences(), sub.newChild(100));
		}
		Set<IMetadataRepository> set = new HashSet<>();
		set.addAll(repos.values());
		return set;
	}

	private void loadMetadataRepository(IMetadataRepositoryManager manager, URI location, HashMap<String, IMetadataRepository> repos, boolean followMetadataRepoReferences, IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		// if we've already processed this repo, don't do it again.  This keeps us from getting
		// caught up in circular references.
		if (repos.containsKey(location.toString()) || failedMetadataRepositories.contains(location)) {
			return;
		}

		SubMonitor sub = SubMonitor.convert(monitor, 1000);
		// First load the repository itself.
		IMetadataRepository repository = loadedMetadataRepositories.get(location);
		if (repository == null) {
			try {
				repository = manager.loadRepository(location, sub.newChild(500));
				loadedMetadataRepositories.put(location, repository);
			} catch (ProvisionException e) {
				failedMetadataRepositories.add(location);
				return;
			}
		}
		repos.put(location.toString(), repository);
		Collection<IRepositoryReference> references = repository.getReferences();
		// We always load artifact repositories referenced by this repository.  We might load
		// metadata repositories
		if (references.size() > 0) {
			IArtifactRepositoryManager artifactManager = agent.getService(IArtifactRepositoryManager.class);
			SubMonitor repoSubMon = SubMonitor.convert(sub.newChild(500), 100 * references.size());
			for (IRepositoryReference ref : references) {
				try {
					if (ref.getType() == IRepository.TYPE_METADATA && followMetadataRepoReferences && isEnabled(manager, ref)) {
						loadMetadataRepository(manager, ref.getLocation(), repos, followMetadataRepoReferences, repoSubMon.newChild(100));
					} else if (ref.getType() == IRepository.TYPE_ARTIFACT) {
						// We want to remember all enabled artifact repository locations.
						if (isEnabled(artifactManager, ref))
							referencedArtifactRepositories.put(ref.getLocation().toString(), ref.getLocation());
					}
				} catch (IllegalArgumentException e) {
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=311338
					// ignore invalid location and keep going
				}
			}
		} else {
			sub.done();
		}

	}

	// If the manager knows about the repo, consider its enablement state in the manager.
	// If the manager does not know about the repo, consider the reference enablement state
	@SuppressWarnings("rawtypes")
	private boolean isEnabled(IRepositoryManager manager, IRepositoryReference reference) {
		return (manager.contains(reference.getLocation()) && manager.isEnabled(reference.getLocation())) || ((!manager.contains(reference.getLocation())) && ((reference.getOptions() & IRepository.ENABLED) == IRepository.ENABLED));
	}

	private boolean shouldFollowReferences() {
		return Boolean.parseBoolean(getProperty(FOLLOW_REPOSITORY_REFERENCES));
	}

	private boolean shouldFollowArtifactReferences() {
		return Boolean.parseBoolean(getProperty(FOLLOW_ARTIFACT_REPOSITORY_REFERENCES));
	}

	/**
	 * Returns a queryable that can be used to obtain any metadata (installable units)
	 * that are needed for the provisioning operation.
	 *
	 * The provisioning context has a distinct lifecycle, whereby the metadata
	 * and artifact repositories to be used are determined when the client retrieves
	 * retrieves the metadata queryable.  Clients should not reset the list of
	 * metadata repository locations or artifact repository locations once the
	 * metadata queryable has been retrieved.
	 *
	 * @param monitor a progress monitor to be used when creating the queryable
	 * @return a queryable that can be used to query available metadata.
	 *
	 * @see #setMetadataRepositories(URI[])
	 * @see #FOLLOW_REPOSITORY_REFERENCES
	 */
	public IQueryable<IInstallableUnit> getMetadata(IProgressMonitor monitor) {
		return QueryUtil.compoundQueryable(getLoadedMetadataRepositories(monitor));
	}

	/**
	 * Returns a map from simple metadata repository location to a subset of the
	 * given installable units available in that repository. All available
	 * {@link #setMetadataRepositories(URI...) metadata repositories} are
	 * considered, including all transitive
	 * {@link ICompositeRepository#getChildren() composite children} and all
	 * {@link IMetadataRepository#getReferences()} where applicable.
	 *
	 * @param ius     the installable units to consider.
	 * @param monitor a progress monitor to be used when querying the repositories.
	 * @return a map from simple metadata repository location to a subset of the
	 *         given installable units available in that repository.
	 * @since 2.8
	 */
	public Map<URI, Set<IInstallableUnit>> getInstallableUnitSources(Collection<? extends IInstallableUnit> ius,
			IProgressMonitor monitor) {
		var result = new TreeMap<URI, Set<IInstallableUnit>>();
		var transport = agent.getService(Transport.class);
		for (var repository : getAllLoadedMetadataRepositories(monitor)) {
			if (repository instanceof ICompositeRepository<?>) {
				continue;
			}
			var location = getSecureLocation(repository.getLocation(), transport);
			var repositoryIUs = new TreeSet<>(repository.query(QueryUtil.ALL_UNITS, monitor).toUnmodifiableSet());
			repositoryIUs.retainAll(ius);
			result.put(location, repositoryIUs);
		}
		return result;
	}

	private URI getSecureLocation(URI uri, Transport transport) {
		try {
			return transport.getSecureLocation(uri);
		} catch (CoreException e) {
			return uri;
		}
	}

	private interface Manager<T> {
		T loadRepository(URI location, IProgressMonitor monitor) throws ProvisionException;
	}

	private Collection<IMetadataRepository> getAllLoadedMetadataRepositories(IProgressMonitor monitor) {
		if (allLoadedMetadataRepositories == null) {
			var repoManager = agent.getService(IMetadataRepositoryManager.class);
			getLoadedMetadataRepositories(monitor);
			allLoadedMetadataRepositories = getAllLoadedRepositories(repoManager::loadRepository,
					loadedMetadataRepositories, failedMetadataRepositories, monitor);
		}
		return allLoadedMetadataRepositories.values();
	}

	private Collection<IArtifactRepository> getAllLoadedArtifactRepositories(IProgressMonitor monitor) {
		if (allLoadedArtifactRepositories == null) {
			var repoManager = agent.getService(IArtifactRepositoryManager.class);
			getLoadedArtifactRepositories(monitor);
			allLoadedArtifactRepositories = getAllLoadedRepositories(repoManager::loadRepository,
					loadedArtifactRepositories, failedArtifactRepositories, monitor);
		}
		return allLoadedArtifactRepositories.values();
	}

	private <T> Map<URI, T> getAllLoadedRepositories(Manager<T> manager,
			Map<URI, T> loadedRepositories,
			Set<URI> failedRepositories,
			IProgressMonitor monitor) {
		var allLoadedRepositories = new HashMap<>(loadedRepositories);
		if (!loadedRepositories.isEmpty()) {
			for (var repository : loadedRepositories.values()) {
				loadComposites(manager, repository, allLoadedRepositories, failedRepositories, monitor);
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
			}
		}
		return allLoadedRepositories;
	}

	private <T> void loadComposites(Manager<T> manager, T repository, Map<URI, T> repos, Set<URI> failedRepositories,
			IProgressMonitor monitor) {
		if (repository instanceof ICompositeRepository<?> composite) {
			for (var location : composite.getChildren()) {
				loadRepository(manager, location, repos, failedRepositories, monitor);
			}
		}
	}

	private <T> void loadRepository(Manager<T> manager, URI location, Map<URI, T> repos,
			Set<URI> failedRepositories, IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}

		if (repos.containsKey(location) || failedMetadataRepositories.contains(location)) {
			return;
		}

		var repository = repos.get(location);
		if (repository == null) {
			try {
				repository = manager.loadRepository(location, monitor);
				repos.put(location, repository);
				loadComposites(manager, repository, repos, failedRepositories, monitor);
			} catch (ProvisionException e) {
				failedMetadataRepositories.add(location);
				return;
			}
		}
	}

	private static Comparator<IArtifactKey> ARTIFACT_KEY_COMPARATOR = (o1, o2) -> {
		var cmp = o1.getId().compareTo(o2.getId());
		if (cmp == 0)
			cmp = o1.getVersion().compareTo(o2.getVersion());
		return cmp;
	};

	/**
	 * Returns a map from simple artifact repository location to a subset of the
	 * given artifact keys available in that repository. All available
	 * {@link #setArtifactRepositories(URI...) artifacts repositories} are
	 * considered, including all transitive
	 * {@link ICompositeRepository#getChildren() composite children} and all
	 * {@link IMetadataRepository#getReferences()} where applicable.
	 *
	 * @param keys    the artifact keys to consider.
	 * @param monitor a progress monitor to be used when querying the repositories.
	 *
	 * @return a map from simple artifact repository location to a subset of the
	 *         given artifact keys available in that repository.
	 * @since 2.8
	 */
	public Map<URI, Set<IArtifactKey>> getArtifactSources(Collection<? extends IArtifactKey> keys,
			IProgressMonitor monitor) {
		var transport = agent.getService(Transport.class);
		var result = new TreeMap<URI, Set<IArtifactKey>>();
		for (var repository : getAllLoadedArtifactRepositories(monitor)) {
			if (repository instanceof ICompositeRepository<?>) {
				continue;
			}
			var location = getSecureLocation(repository.getLocation(), transport);
			for (var key : keys) {
				if (repository.contains(key)) {
					result.computeIfAbsent(location, it -> new TreeSet<>(ARTIFACT_KEY_COMPARATOR)).add(key);
				}
			}
		}
		return result;
	}

	/**
	 * Returns the list of additional installable units that should be considered as
	 * available for installation by the planner. Returns an empty list if
	 * there are no extra installable units to consider. This method has no effect on the
	 * execution of the engine.
	 *
	 * @return The extra installable units that are available
	 */
	public List<IInstallableUnit> getExtraInstallableUnits() {
		return extraIUs;
	}

	/**
	 * Returns the properties that are defined in this context. Context properties can
	 * be used to influence the behavior of either the planner or engine.
	 *
	 * @return the defined provisioning context properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Returns the value of the property with the given key, or <code>null</code>
	 * if no such property is defined
	 * @param key the property key
	 * @return the property value, or <code>null</code>
	 */
	public String getProperty(String key) {
		return properties.get(key);
	}

	/**
	 * Sets the artifact repositories to consult when performing an operation.
	 * <p>
	 * The provisioning context has a distinct lifecycle, whereby the metadata
	 * and artifact repositories to be used are determined when the client
	 * retrieves the metadata queryable.  Clients should not reset the list of
	 * artifact repository locations once the metadata queryable has been retrieved.
	 *
	 * @param artifactRepositories the artifact repository locations
	*/
	public void setArtifactRepositories(URI... artifactRepositories) {
		this.artifactRepositories = artifactRepositories;
	}

	/**
	 * Sets the metadata repositories to consult when performing an operation.
	 * <p>
	 * The provisioning context has a distinct lifecycle, whereby the metadata
	 * and artifact repositories to be used are determined when the client
	 * retrieves the metadata queryable.  Clients should not reset the list of
	 * metadata repository locations once the metadata queryable has been retrieved.
	 * @param metadataRepositories the metadata repository locations
	*/
	public void setMetadataRepositories(URI... metadataRepositories) {
		this.metadataRepositories = metadataRepositories;
	}

	/**
	 * Sets the list of additional installable units that should be considered as
	 * available for installation by the planner. This method has no effect on the
	 * execution of the engine.
	 * @param extraIUs the extra installable units
	 */
	public void setExtraInstallableUnits(List<IInstallableUnit> extraIUs) {
		this.extraIUs.clear();
		//copy the list to prevent future client tampering
		if (extraIUs != null)
			this.extraIUs.addAll(extraIUs);
	}

	/**
	 * Sets a property on this provisioning context. Context properties can
	 * be used to influence the behavior of either the planner or engine.
	 * @param key the property key
	 * @param value the property value
	 */
	public void setProperty(String key, String value) {
		properties.put(key, value);
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("{artifactRepos=" + DebugHelper.formatArray(null != artifactRepositories ? Arrays.asList(artifactRepositories) : null, true, false)); //$NON-NLS-1$
		buffer.append(", metadataRepos=" + DebugHelper.formatArray(null != metadataRepositories ? Arrays.asList(metadataRepositories) : null, true, false)); //$NON-NLS-1$
		buffer.append(", properties=" + getProperties() + "}"); //$NON-NLS-1$ //$NON-NLS-2$
		return buffer.toString();
	}
}
