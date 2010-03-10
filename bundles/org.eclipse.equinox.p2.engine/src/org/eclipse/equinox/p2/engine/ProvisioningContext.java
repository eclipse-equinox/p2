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
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
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
	private Collection<IRequirement> additionalRequirements;
	private URI[] artifactRepositories; //artifact repositories to consult
	private final List<IInstallableUnit> extraIUs = Collections.synchronizedList(new ArrayList<IInstallableUnit>());
	private URI[] metadataRepositories; //metadata repositories to consult
	private final Map<String, String> properties = new HashMap<String, String>();

	private static final String FILE_PROTOCOL = "file"; //$NON-NLS-1$

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
	 * Returns a collection of additional requirements that must be satisfied by the planner,
	 * or <code>null</code> if there are no additional requirements.
	 * This method has no effect on the execution of the engine.
	 * 
	 * @return a collection of additional requirements
	 */
	public Collection<IRequirement> getAdditionalRequirements() {
		return additionalRequirements;
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
		IArtifactRepositoryManager repoManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		URI[] repositories = getArtifactRepositories();
		List<IArtifactRepository> repos = new ArrayList<IArtifactRepository>();
		SubMonitor sub = SubMonitor.convert(monitor, repositories.length * 100);
		try {
			for (int i = 0; i < repositories.length; i++) {
				if (sub.isCanceled())
					throw new OperationCanceledException();
				repos.add(repoManager.loadRepository(repositories[i], sub.newChild(100)));
			}
		} catch (ProvisionException e) {
			//skip unreadable repositories
		}
		return QueryUtil.compoundQueryable(repos);
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
		IArtifactRepositoryManager repoManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		URI[] repositories = getArtifactRepositories();
		List<IQueryable<IArtifactDescriptor>> descriptorQueryables = new ArrayList<IQueryable<IArtifactDescriptor>>();
		SubMonitor sub = SubMonitor.convert(monitor, repositories.length * 100);
		try {
			for (int i = 0; i < repositories.length; i++) {
				if (sub.isCanceled())
					throw new OperationCanceledException();
				IArtifactRepository repo = repoManager.loadRepository(repositories[i], sub.newChild(100));
				descriptorQueryables.add(repo.descriptorQueryable());
			}
		} catch (ProvisionException e) {
			//skip unreadable repositories
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
		IArtifactRepositoryManager repoManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		URI[] repositories = getArtifactRepositories();
		final List<IArtifactRepository> repos = new ArrayList<IArtifactRepository>();
		SubMonitor sub = SubMonitor.convert(monitor, repositories.length * 100);
		try {
			for (int i = 0; i < repositories.length; i++) {
				if (sub.isCanceled())
					throw new OperationCanceledException();
				repos.add(repoManager.loadRepository(repositories[i], sub.newChild(100)));
			}
		} catch (ProvisionException e) {
			//skip unreadable repositories
		}
		return new IQueryable<IArtifactRepository>() {
			public IQueryResult<IArtifactRepository> query(IQuery<IArtifactRepository> query, IProgressMonitor mon) {
				return query.perform(repos.iterator());
			}

		};
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
		IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		URI[] repositories = metadataRepositories == null ? repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL) : metadataRepositories;
		List<IMetadataRepository> repos = new ArrayList<IMetadataRepository>();
		SubMonitor sub = SubMonitor.convert(monitor, repositories.length * 100);
		try {
			for (int i = 0; i < repositories.length; i++) {
				if (sub.isCanceled())
					throw new OperationCanceledException();
				repos.add(repoManager.loadRepository(repositories[i], sub.newChild(100)));
			}
		} catch (ProvisionException e) {
			//skip unreadable repositories
		}
		return QueryUtil.compoundQueryable(repos);
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
	 * Sets the additional requirements that must be satisfied by the planner.
	 * This method has no effect on the execution of the engine.
	 * 
	 * @param requirements the additional requirements
	 */
	public void setAdditionalRequirements(Collection<IRequirement> requirements) {
		additionalRequirements = requirements;
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
		buffer.append("{artifactRepos=" + DebugHelper.formatArray(Arrays.asList(artifactRepositories), true, false)); //$NON-NLS-1$
		buffer.append(", metadataRepos=" + DebugHelper.formatArray(Arrays.asList(metadataRepositories), true, false)); //$NON-NLS-1$
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
