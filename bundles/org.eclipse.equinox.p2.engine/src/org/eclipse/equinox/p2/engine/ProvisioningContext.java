/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 * 	IBM Corporation - initial API and implementation
 *     WindRiver - https://bugs.eclipse.org/bugs/show_bug.cgi?id=227372
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.DebugHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;

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
	private final Map<String, String> properties = new HashMap<String, String>();
	private Map<String, IRepositoryReference> metadataRepositorySnapshot = null;
	private Map<String, IRepositoryReference> artifactRepositorySnapshot = null;
	private Map<String, IArtifactRepository> referencedArtifactRepositories = null;

	private static final String FILE_PROTOCOL = "file"; //$NON-NLS-1$

	class ArtifactRepositoryQueryable implements IQueryable<IArtifactRepository> {
		List<IArtifactRepository> repositories;

		ArtifactRepositoryQueryable(List<IArtifactRepository> repositories) {
			this.repositories = repositories;
		}

		public IQueryResult<IArtifactRepository> query(IQuery<IArtifactRepository> query, IProgressMonitor mon) {
			return query.perform(repositories.listIterator());
		}
	}

	/**
	 * This Comparator sorts the repositories such that local repositories are first
	 */
	private static final Comparator<URI> LOCAL_FIRST_COMPARATOR = new Comparator<URI>() {

		public int compare(URI arg0, URI arg1) {
			String protocol0 = arg0.getScheme();
			String protocol1 = arg1.getScheme();

			if (FILE_PROTOCOL.equals(protocol0) && !FILE_PROTOCOL.equals(protocol1))
				return -1;
			if (!FILE_PROTOCOL.equals(protocol0) && FILE_PROTOCOL.equals(protocol1))
				return 1;
			return 0;
		}
	};

	/**
	 * Instructs the provisioning context to follow repository references when providing
	 * queryables for obtaining metadata and artifacts.  When this property is set to
	 * "true", then both enabled and disabled repository references that are encountered
	 * while loading the specified metadata repositories will be included in the provisioning
	 * context.  In this mode, the provisioning context has a distinct lifecycle, whereby
	 * the metadata and artifact repositories to be used are determined when the client
	 * retrieves the metadata queryable.  Clients using this property should not reset the
	 * list of metadata repository locations or artifact repository locations once the
	 * metadata queryable has been retrieved.
	 *
	 * @see #getMetadata(IProgressMonitor)
	 * @see #setMetadataRepositories(URI[])
	 * @see #setArtifactRepositories(URI[])
	 */
	public static final String FOLLOW_REPOSITORY_REFERENCES = "org.eclipse.equinox.p2.director.followRepositoryReferences"; //$NON-NLS-1$

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
	}

	/**
	 * Returns a queryable that can be used to obtain any artifact keys that
	 * are needed for the provisioning operation.
	 *
	 * @param monitor a progress monitor to be used when creating the queryable
	 * @return a queryable that can be used to query available artifact keys.
	 *
	 * @see #setArtifactRepositories(URI[])
	 * @see #FOLLOW_REPOSITORY_REFERENCES
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
	 * @see #FOLLOW_REPOSITORY_REFERENCES
	 */
	public IQueryable<IArtifactDescriptor> getArtifactDescriptors(IProgressMonitor monitor) {
		List<IArtifactRepository> repos = getLoadedArtifactRepositories(monitor);
		List<IQueryable<IArtifactDescriptor>> descriptorQueryables = new ArrayList<IQueryable<IArtifactDescriptor>>();
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
	 * @see #FOLLOW_REPOSITORY_REFERENCES
	 */
	public IQueryable<IArtifactRepository> getArtifactRepositories(IProgressMonitor monitor) {
		return new ArtifactRepositoryQueryable(getLoadedArtifactRepositories(monitor));
	}

	/**
	 * Return an array of loaded artifact repositories.
	 */
	private List<IArtifactRepository> getLoadedArtifactRepositories(IProgressMonitor monitor) {
		IArtifactRepositoryManager repoManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		URI[] repositories = artifactRepositories == null ? repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL) : artifactRepositories;
		Arrays.sort(repositories, LOCAL_FIRST_COMPARATOR);

		List<IArtifactRepository> repos = new ArrayList<IArtifactRepository>();
		SubMonitor sub = SubMonitor.convert(monitor, repositories.length * 100);
		for (int i = 0; i < repositories.length; i++) {
			if (sub.isCanceled())
				throw new OperationCanceledException();
			try {
				repos.add(repoManager.loadRepository(repositories[i], sub.newChild(100)));
			} catch (ProvisionException e) {
				//skip unreadable repositories
			}
		}
		if (referencedArtifactRepositories != null)
			for (IArtifactRepository repo : referencedArtifactRepositories.values())
				repos.add(repo);
		return repos;
	}

	private Set<IMetadataRepository> getLoadedMetadataRepositories(IProgressMonitor monitor) {
		IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		URI[] repositories = metadataRepositories == null ? repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL) : metadataRepositories;
		Set<IMetadataRepository> repos = new HashSet<IMetadataRepository>();
		SubMonitor sub = SubMonitor.convert(monitor, repositories.length * 100 + 100);
		// We always load the repositories explicitly specified first.  This way, the side effects of loading 
		// the top level repositories (reading repository references and remembering them in the manager)
		// are the same regardless of whether we choose to follow those references.
		for (int i = 0; i < repositories.length; i++) {
			if (sub.isCanceled())
				throw new OperationCanceledException();
			try {
				repos.add(repoManager.loadRepository(repositories[i], sub.newChild(100)));
			} catch (ProvisionException e) {
				//skip unreadable repositories
			}
		}
		if (!shouldFollowReferences()) {
			sub.done();
			return repos;
		}
		// Those last 100 ticks are now converted to be based on the repository reference following
		sub = SubMonitor.convert(sub.newChild(100), 100 * repositories.length);
		// Snapshot the repository state.  Anything else we enable or add as part of traversing references should be
		// forgotten when we are done.
		snapShotRepositoryState();
		// We need to remember the loaded artifact repositories because we will be 
		// restoring the enable/disable state of the referenced repos in the manager after traversing
		// the metadata repos.  
		referencedArtifactRepositories = new HashMap<String, IArtifactRepository>();

		for (int i = 0; i < repositories.length; i++) {
			if (sub.isCanceled())
				throw new OperationCanceledException();
			loadMetadataRepository(repoManager, repositories[i], repos, shouldFollowReferences(), sub.newChild(100));
		}
		restoreRepositoryState();
		return repos;
	}

	private void snapShotRepositoryState() {
		metadataRepositorySnapshot = new HashMap<String, IRepositoryReference>();
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		List<URI> all = new ArrayList<URI>();
		all.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL)));
		all.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED)));
		for (URI location : all) {
			int options = manager.isEnabled(location) ? IRepository.ENABLED : IRepository.NONE;
			metadataRepositorySnapshot.put(location.toString(), new RepositoryReference(location, manager.getRepositoryProperty(location, IRepository.PROP_NICKNAME), IRepository.TYPE_METADATA, options));
		}
		artifactRepositorySnapshot = new HashMap<String, IRepositoryReference>();
		IArtifactRepositoryManager artManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		all = new ArrayList<URI>();
		all.addAll(Arrays.asList(artManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL)));
		all.addAll(Arrays.asList(artManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED)));
		for (URI location : all) {
			int options = artManager.isEnabled(location) ? IRepository.ENABLED : IRepository.NONE;
			artifactRepositorySnapshot.put(location.toString(), new RepositoryReference(location, artManager.getRepositoryProperty(location, IRepository.PROP_NICKNAME), IRepository.TYPE_ARTIFACT, options));
		}
	}

	private void restoreRepositoryState() {
		if (metadataRepositorySnapshot != null) {
			IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
			List<URI> all = new ArrayList<URI>();
			all.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL)));
			all.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED)));
			for (URI location : all) {
				IRepositoryReference reference = metadataRepositorySnapshot.get(location.toString());
				if (reference == null) {
					manager.removeRepository(location);
				} else {
					manager.setEnabled(location, (reference.getOptions() & IRepository.ENABLED) == IRepository.ENABLED);
					manager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, reference.getNickname());
					metadataRepositorySnapshot.remove(location);
				}
			}
			// Anything left in the map is no longer known by the manager, so add it back.  (This is not likely)
			for (IRepositoryReference ref : metadataRepositorySnapshot.values()) {
				manager.addRepository(ref.getLocation());
				manager.setEnabled(ref.getLocation(), (ref.getOptions() & IRepository.ENABLED) == IRepository.ENABLED);
				manager.setRepositoryProperty(ref.getLocation(), IRepository.PROP_NICKNAME, ref.getNickname());
			}
			metadataRepositorySnapshot = null;
		}
		if (artifactRepositorySnapshot != null) {
			IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
			List<URI> all = new ArrayList<URI>();
			all.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL)));
			all.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED)));
			for (URI location : all) {
				IRepositoryReference reference = artifactRepositorySnapshot.get(location.toString());
				if (reference == null) {
					manager.removeRepository(location);
				} else {
					manager.setEnabled(location, (reference.getOptions() & IRepository.ENABLED) == IRepository.ENABLED);
					manager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, reference.getNickname());
					artifactRepositorySnapshot.remove(location);
				}
			}
			// Anything left in the map is no longer known by the manager, so add it back. (This is not likely)
			for (IRepositoryReference ref : artifactRepositorySnapshot.values()) {
				manager.addRepository(ref.getLocation());
				manager.setEnabled(ref.getLocation(), (ref.getOptions() & IRepository.ENABLED) == IRepository.ENABLED);
				manager.setRepositoryProperty(ref.getLocation(), IRepository.PROP_NICKNAME, ref.getNickname());
			}
			artifactRepositorySnapshot = null;
		}
	}

	private void loadMetadataRepository(IMetadataRepositoryManager manager, URI location, Set<IMetadataRepository> repos, boolean followReferences, IProgressMonitor monitor) {
		try {
			if (!followReferences) {
				repos.add(manager.loadRepository(location, monitor));
				return;
			}
			// We want to load all repositories referenced by this repository
			SubMonitor sub = SubMonitor.convert(monitor, 1000);
			IMetadataRepository repository = manager.loadRepository(location, sub.newChild(500));
			repos.add(repository);
			Collection<IRepositoryReference> references = repository.getReferences();
			if (references.size() > 0) {
				IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
				SubMonitor repoSubMon = SubMonitor.convert(sub.newChild(500), 100 * references.size());
				for (IRepositoryReference ref : references) {
					if (ref.getType() == IRepository.TYPE_METADATA) {
						loadMetadataRepository(manager, ref.getLocation(), repos, followReferences, repoSubMon.newChild(100));
					} else {
						// keyed by location so that duplicate instances are treated as one.  We can't rely on "equals"
						referencedArtifactRepositories.put(ref.getLocation().toString(), artifactManager.loadRepository(ref.getLocation(), repoSubMon.newChild(100)));
					}
				}
			} else {
				sub.done();
			}
		} catch (ProvisionException e) {
			//skip unreadable repositories
		}
	}

	private boolean shouldFollowReferences() {
		return Boolean.valueOf(getProperty(FOLLOW_REPOSITORY_REFERENCES)).booleanValue();
	}

	/**
	 * Returns a queryable that can be used to obtain any metadata (installable units)
	 * that are needed for the provisioning operation.
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
	 * When the {@link #FOLLOW_REPOSITORY_REFERENCES} property is set, this
	 * method should be called prior to calling {@link #getMetadata(IProgressMonitor)},
	 * because setting the repositories after retrieving metadata will have no
	 * effect.
	 *
	 * @param artifactRepositories the artifact repository locations
	 * @see #FOLLOW_REPOSITORY_REFERENCES
	*/
	public void setArtifactRepositories(URI[] artifactRepositories) {
		this.artifactRepositories = artifactRepositories;
	}

	/**
	 * Sets the metadata repositories to consult when performing an operation.
	 * @param metadataRepositories the metadata repository locations
	*/
	public void setMetadataRepositories(URI[] metadataRepositories) {
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

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("{artifactRepos=" + DebugHelper.formatArray(null != artifactRepositories ? Arrays.asList(artifactRepositories) : null, true, false)); //$NON-NLS-1$
		buffer.append(", metadataRepos=" + DebugHelper.formatArray(null != metadataRepositories ? Arrays.asList(metadataRepositories) : null, true, false)); //$NON-NLS-1$
		buffer.append(", properties=" + getProperties() + "}"); //$NON-NLS-1$ //$NON-NLS-2$
		return buffer.toString();
	}

	/**
	 * Return the array of repository locations for artifact repositories.
	 * @return an array of repository locations.  This is never <code>null</code>.
	 *
	 * @deprecated This method will be removed before the final release of 3.6
	 * @noreference This method will be removed before the final release of 3.6
	 * @see #getArtifactRepositories()
	 * @see #getArtifactDescriptors(IProgressMonitor)
	 * @see #getArtifactKeys(IProgressMonitor)
	 */
	public URI[] getArtifactRepositories() {
		IArtifactRepositoryManager repoManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		URI[] repositories = artifactRepositories == null ? repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL) : artifactRepositories;
		Arrays.sort(repositories, LOCAL_FIRST_COMPARATOR);
		return repositories;
	}
}
