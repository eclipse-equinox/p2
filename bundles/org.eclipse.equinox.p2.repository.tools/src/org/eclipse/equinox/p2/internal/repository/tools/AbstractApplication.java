/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;
import java.util.function.Predicate;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class AbstractApplication {
	protected boolean removeAddedRepositories = true;

	protected List<RepositoryDescriptor> sourceRepositories = new ArrayList<>(); // List of repository descriptors
	protected List<URI> artifactReposToRemove = new ArrayList<>();
	protected List<URI> metadataReposToRemove = new ArrayList<>();
	protected List<IInstallableUnit> sourceIUs = new ArrayList<>();
	private List<RepositoryDescriptor> destinationRepos = new ArrayList<>();

	protected IArtifactRepository destinationArtifactRepository = null;
	protected IMetadataRepository destinationMetadataRepository = null;

	private CompositeMetadataRepository compositeMetadataRepository = null;
	private CompositeArtifactRepository compositeArtifactRepository = null;

	protected IProvisioningAgent agent;

	public AbstractApplication() {
		super();
		try {
			setupAgent();
		} catch (ProvisionException e) {
			LogHelper.log(e);
		}
	}

	public AbstractApplication(IProvisioningAgent agent) {
		this.agent = agent;
	}

	private void setupAgent() throws ProvisionException {
		// note if we ever wanted these applications to act on a different agent than
		// the currently running system we would need to set it here
		BundleContext bundleContext = Activator.getBundleContext();
		if (bundleContext == null) {
			return;
		}
		ServiceReference<IProvisioningAgent> agentRef = bundleContext
				.getServiceReference(IProvisioningAgent.class);
		if (agentRef != null) {
			agent = bundleContext.getService(agentRef);
			if (agent != null)
				return;
		}
		// there is no agent around so we need to create one
		ServiceReference<IProvisioningAgentProvider> providerRef = bundleContext
				.getServiceReference(IProvisioningAgentProvider.class);
		if (providerRef == null)
			throw new RuntimeException("No provisioning agent provider is available"); //$NON-NLS-1$
		IProvisioningAgentProvider provider = bundleContext.getService(providerRef);
		if (provider == null)
			throw new RuntimeException("No provisioning agent provider is available"); //$NON-NLS-1$
		// obtain agent for currently running system
		agent = provider.createAgent(null);
		bundleContext.ungetService(providerRef);
	}

	public void setSourceIUs(List<IInstallableUnit> ius) {
		sourceIUs = ius;
	}

	protected void finalizeRepositories() {
		if (removeAddedRepositories) {
			IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
			for (URI uri : artifactReposToRemove) {
				artifactRepositoryManager.removeRepository(uri);
			}
			IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
			for (URI uri : metadataReposToRemove) {
				metadataRepositoryManager.removeRepository(uri);
			}
		}
		metadataReposToRemove = null;
		artifactReposToRemove = null;
		compositeArtifactRepository = null;
		compositeMetadataRepository = null;
		destinationArtifactRepository = null;
		destinationMetadataRepository = null;
	}

	protected IMetadataRepositoryManager getMetadataRepositoryManager() {
		return agent.getService(IMetadataRepositoryManager.class);
	}

	protected IArtifactRepositoryManager getArtifactRepositoryManager() {
		return agent.getService(IArtifactRepositoryManager.class);
	}

	public void initializeRepos(IProgressMonitor progress) throws ProvisionException {
		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		URI curLocation = null;
		for (RepositoryDescriptor repo : sourceRepositories) {
			try {
				curLocation = repo.getRepoLocation();
				if (repo.isBoth()) {
					addRepository(artifactRepositoryManager, curLocation, 0, artifactReposToRemove, progress);
					addRepository(metadataRepositoryManager, curLocation, 0, metadataReposToRemove, progress);
				} else if (repo.isArtifact()) {
					addRepository(artifactRepositoryManager, curLocation, 0, artifactReposToRemove, progress);
				} else if (repo.isMetadata()) {
					addRepository(metadataRepositoryManager, curLocation, 0, metadataReposToRemove, progress);
				} else {
					throw new ProvisionException(NLS.bind(Messages.unknown_repository_type, repo.getRepoLocation()));
				}
			} catch (ProvisionException e) {
				if (e.getCause() instanceof MalformedURLException) {
					throw new ProvisionException(NLS.bind(Messages.exception_invalidSource, curLocation), e);
				} else if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND && repo.isOptional()) {
					continue;
				}
				throw e;
			}
		}
		processDestinationRepos(artifactRepositoryManager, metadataRepositoryManager);
	}

	// Helper to add a repository. It takes care of adding the repos to the deletion
	// list and loading it
	protected <T> IRepository<T> addRepository(IRepositoryManager<T> manager, URI location, int flags,
			List<URI> repoList, IProgressMonitor monitor) throws ProvisionException {
		if (!manager.contains(location)) {
			repoList.add(location);
		}
		return manager.loadRepository(location, flags, monitor);
	}

	private void processDestinationRepos(IArtifactRepositoryManager artifactRepositoryManager,
			IMetadataRepositoryManager metadataRepositoryManager) throws ProvisionException {

		destinationArtifactRepository = (IArtifactRepository) initializeDestinationRepo(artifactRepositoryManager,
				RepositoryDescriptor::isArtifact);
		destinationMetadataRepository = (IMetadataRepository) initializeDestinationRepo(metadataRepositoryManager,
				RepositoryDescriptor::isMetadata);
		if (destinationMetadataRepository == null && destinationArtifactRepository == null) {
			throw new ProvisionException(Messages.AbstractApplication_no_valid_destinations);
		}
	}

	private <T> IRepository<T> initializeDestinationRepo(IRepositoryManager<T> repositoryManager,
			Predicate<RepositoryDescriptor> typeFilter) throws ProvisionException {
		Optional<RepositoryDescriptor> descriptor = destinationRepos.stream().filter(typeFilter).findFirst();
		if (descriptor.isPresent()) {
			return initializeDestination(descriptor.get(), repositoryManager);
		}
		return null;
	}

	public IMetadataRepository getDestinationMetadataRepository() {
		return destinationMetadataRepository;
	}

	public IArtifactRepository getDestinationArtifactRepository() {
		return destinationArtifactRepository;
	}

	protected <T> IRepository<T> initializeDestination(RepositoryDescriptor toInit, IRepositoryManager<T> mgr)
			throws ProvisionException {

		String repositoryType;
		List<URI> repoList;
		if (mgr instanceof IMetadataRepositoryManager) {
			repositoryType = IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY;
			repoList = metadataReposToRemove;
		} else if (mgr instanceof IArtifactRepositoryManager) {
			repositoryType = IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY;
			repoList = artifactReposToRemove;
		} else  {
			throw new AssertionError("Unsupported repository type: " + mgr.getClass()); //$NON-NLS-1$
		}
		try {
			IRepository<T> repository = addRepository(mgr, toInit.getRepoLocation(),
					IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, repoList, null);
			if (initDestinationRepository(repository, toInit)) {
				return repository;
			}
		} catch (ProvisionException e) {
			// fall through and create a new repository below
		}
		IRepository<T> source = null;
		try {
			if (toInit.getFormat() != null) {
				source = mgr.loadRepository(toInit.getFormat(), 0, null);
			}
		} catch (ProvisionException e) {
			// Ignore.
		}
		// This code assumes source has been successfully loaded before this point
		// No existing repository; create a new repository at destinationLocation but
		// with source's attributes.
		// TODO for now create a Simple repo by default.
		try {
			IRepository<T> result = mgr.createRepository(toInit.getRepoLocation(),
					toInit.getName() != null ? toInit.getName()
							: (source != null ? source.getName() : toInit.getRepoLocation().toString()),
					repositoryType, source != null ? source.getProperties() : null);
			if (toInit.isCompressed() && !result.getProperties().containsKey(IRepository.PROP_COMPRESSED))
				result.setProperty(IRepository.PROP_COMPRESSED, "true"); //$NON-NLS-1$
			return RepositoryHelper.validDestinationRepository(result);
		} catch (UnsupportedOperationException e) {
			throw new ProvisionException(NLS.bind(Messages.exception_invalidDestination, toInit.getRepoLocation()),
					e.getCause());
		}
	}

	protected boolean initDestinationRepository(IRepository<?> repository, RepositoryDescriptor descriptor) {
		if (repository != null && repository.isModifiable()) {
			if (descriptor.getName() != null) {
				repository.setProperty(IRepository.PROP_NAME, descriptor.getName());
			}
			if (repository instanceof ICompositeRepository<?> && !descriptor.isAppend()) {
				((ICompositeRepository<?>) repository).removeAllChildren();
			} else if (repository instanceof IMetadataRepository metadataRepository && !descriptor.isAppend()) {
				metadataRepository.removeAll();
			} else if (repository instanceof IArtifactRepository artifactRepository && !descriptor.isAppend()) {
				artifactRepository.removeAll();
			}
			return true;
		}
		return false;
	}

	public IMetadataRepository getCompositeMetadataRepository() {
		if (compositeMetadataRepository == null) {
			compositeMetadataRepository = CompositeMetadataRepository.createMemoryComposite(agent);
			if (compositeMetadataRepository != null) {
				for (RepositoryDescriptor repo : sourceRepositories) {
					if (repo.isMetadata())
						compositeMetadataRepository.addChild(repo.getRepoLocation());
				}
			}
		}
		return compositeMetadataRepository;
	}

	public IArtifactRepository getCompositeArtifactRepository() {
		if (compositeArtifactRepository == null) {
			compositeArtifactRepository = CompositeArtifactRepository.createMemoryComposite(agent);
			if (compositeArtifactRepository != null) {
				for (RepositoryDescriptor repo : sourceRepositories) {
					if (repo.isArtifact())
						compositeArtifactRepository.addChild(repo.getRepoLocation());
				}
			}
		}
		return compositeArtifactRepository;
	}

	public boolean hasArtifactSources() {
		return !((ICompositeRepository<?>) getCompositeArtifactRepository()).getChildren().isEmpty();
	}

	public boolean hasMetadataSources() {
		return !((ICompositeRepository<?>) getCompositeMetadataRepository()).getChildren().isEmpty();
	}

	public abstract IStatus run(IProgressMonitor monitor) throws ProvisionException;

	public void addDestination(RepositoryDescriptor descriptor) {
		destinationRepos.add(descriptor);
	}

	public void addSource(RepositoryDescriptor repo) {
		sourceRepositories.add(repo);
	}
}