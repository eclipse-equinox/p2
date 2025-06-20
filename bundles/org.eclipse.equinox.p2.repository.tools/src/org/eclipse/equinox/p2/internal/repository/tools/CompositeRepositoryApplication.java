/*******************************************************************************
 * Copyright (c) 2009, 2025 IBM Corporation and others.
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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryState;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;

public class CompositeRepositoryApplication extends AbstractApplication {

	private final List<RepositoryDescriptor> childrenToAdd = new ArrayList<>();
	private final List<RepositoryDescriptor> childrenToRemove = new ArrayList<>();
	private boolean removeAllChildren = false;
	private boolean failOnExists = false;
	private String comparatorID = null;

	public CompositeRepositoryApplication() {
		super();
	}

	public CompositeRepositoryApplication(IProvisioningAgent agent) {
		super(agent);
	}

	@Override
	public IStatus run(IProgressMonitor monitor) throws ProvisionException {
		try {
			initializeRepos(new NullProgressMonitor());
			// load repository
			CompositeMetadataRepository metadataRepo = (CompositeMetadataRepository) destinationMetadataRepository;
			CompositeArtifactRepository artifactRepo = (CompositeArtifactRepository) destinationArtifactRepository;

			if (removeAllChildren) {
				if (artifactRepo != null) {
					artifactRepo.removeAllChildren();
				}
				if (metadataRepo != null) {
					metadataRepo.removeAllChildren();
				}
			} else {
				// Remove children from the Composite Repositories
				for (RepositoryDescriptor child : childrenToRemove) {
					if (child.isArtifact() && artifactRepo != null) {
						artifactRepo.removeChild(child.getOriginalRepoLocation());
					}
					if (child.isMetadata() && metadataRepo != null) {
						metadataRepo.removeChild(child.getOriginalRepoLocation());
					}
				}
			}

			// Add children to the Composite Repositories
			for (RepositoryDescriptor child : childrenToAdd) {
				if (child.isArtifact() && artifactRepo != null) {
					artifactRepo.addChild(child.getOriginalRepoLocation());
				}
				if (child.isMetadata() && metadataRepo != null) {
					metadataRepo.addChild(child.getOriginalRepoLocation());
				}
			}

			if (comparatorID != null) {
				ArtifactRepositoryValidator validator = new ArtifactRepositoryValidator(comparatorID);
				return validator.validateComposite(artifactRepo);
			}
			return Status.OK_STATUS;
		} finally {
			finalizeRepositories();
		}
	}

	public void addChild(RepositoryDescriptor child) {
		childrenToAdd.add(child);
	}

	public void removeChild(RepositoryDescriptor child) {
		childrenToRemove.add(child);
	}

	public void setRemoveAll(boolean all) {
		removeAllChildren = all;
	}

	public void setFailOnExists(boolean value) {
		failOnExists = value;
	}

	@Override
	protected <T> IRepository<T> initializeDestination(RepositoryDescriptor toInit, IRepositoryManager<T> mgr)
			throws ProvisionException {

		URI destinationLocation = toInit.getRepoLocation();
		String repositoryType;
		String defaultName;
		Predicate<RepositoryDescriptor> repositoryTypeFilter;
		if (mgr instanceof IArtifactRepositoryManager) {
			repositoryType = IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY;
			defaultName = Messages.CompositeRepository_default_artifactRepo_name;
			repositoryTypeFilter = RepositoryDescriptor::isArtifact;
		} else if (mgr instanceof IMetadataRepositoryManager) {
			repositoryType = IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY;
			defaultName = Messages.CompositeRepository_default_metadataRepo_name;
			repositoryTypeFilter = RepositoryDescriptor::isMetadata;
		} else {
			throw new AssertionError("Unsupported repository type: " + mgr.getClass()); //$NON-NLS-1$
		}
		// remove the repo first.
		mgr.removeRepository(destinationLocation);

		// first try and load to see if one already exists at that location.
		try {
			IRepository<T> repository = mgr.loadRepository(destinationLocation, null);
			validRepositoryLocation(repository);
			if (!initDestinationRepository(repository, toInit)) {
				throw new ProvisionException(Status
						.info(NLS.bind(Messages.CompositeRepository_composite_repository_exists, destinationLocation)));
			}
			return repository;
		} catch (ProvisionException e) {
			// re-throw the exception if we got anything other than "repo not found"
			if (e.getStatus().getCode() != ProvisionException.REPOSITORY_NOT_FOUND) {
				if (e.getCause() instanceof MalformedURLException) {
					throw new ProvisionException(NLS.bind(Messages.exception_invalidDestination, destinationLocation),
							e.getCause());
				}
				throw e;
			}
		}

		IRepository<T> source = null;
		boolean copyChildren = false;
		try {
			if (toInit.getFormat() != null) {
				source = mgr.loadRepository(toInit.getFormat(), null);
			} else {
				URI sourceRepoLocation = sourceRepositories.stream().filter(repositoryTypeFilter).findFirst()
						.map(RepositoryDescriptor::getRepoLocation).orElse(null);
				if (sourceRepoLocation != null && mgr.contains(sourceRepoLocation)) {
					source = mgr.loadRepository(sourceRepoLocation, null);
					copyChildren = true;
				}
			}
		} catch (ProvisionException e) {
			// Ignore
		}
		// This code assumes source has been successfully loaded before this point
		try {
			// No existing repository; create a new repository at destinationLocation but
			// with source's attributes.
			String name = Optional.ofNullable(toInit.getName()).orElse(source != null ? source.getName() : defaultName);
			IRepository<T> repo = mgr.createRepository(destinationLocation, name, repositoryType,
					source != null ? source.getProperties() : null);
			initRepository(repo, toInit);
			setAtomicLoadingProperty(repo, toInit);

			if (copyChildren && source instanceof ICompositeRepository<?> sourceComposite
					&& repo instanceof ICompositeRepository<?> destinationComposite) {
				List<URI> children = getChildrenOriginalLocation(sourceComposite);
				for (URI childURI : children) {
					destinationComposite.addChild(childURI);
				}
			}

			return repo;
		} catch (IllegalStateException e) {
			mgr.removeRepository(destinationLocation);
			throw e;
		}
	}

	private static List<URI> getChildrenOriginalLocation(ICompositeRepository<?> composite) {
		CompositeRepositoryState state;
		if (composite instanceof CompositeArtifactRepository artifactComposite) {
			state = artifactComposite.toState();
		} else if (composite instanceof CompositeMetadataRepository metadataComposite) {
			state = metadataComposite.toState();
		} else {
			throw new IllegalArgumentException("Unsupported composite repository type: " + composite); //$NON-NLS-1$
		}
		return List.of(state.getChildren());
	}

	/*
	 * Determine if the repository is valid for this operation
	 */
	private void validRepositoryLocation(IRepository<?> repository) throws ProvisionException {
		if (repository instanceof ICompositeRepository<?>) {
			// if we have an already existing repository at that location, then throw an error
			// if the user told us to
			if (failOnExists) {
				throw new ProvisionException(
						NLS.bind(Messages.CompositeRepository_composite_repository_exists, repository.getLocation()));
			}
			RepositoryHelper.validDestinationRepository(repository);
		}
		// we have a non-composite repo at this location. that is ok because we can co-exist.
	}

	private void setAtomicLoadingProperty(IRepository<?> repository, RepositoryDescriptor desc) {
		// bug 356561: newly created repositories shall be atomic (by default)
		boolean atomic = true;
		if (desc.getAtomic() != null) {
			atomic = Boolean.valueOf(desc.getAtomic());
		}
		repository.setProperty(CompositeMetadataRepository.PROP_ATOMIC_LOADING, Boolean.toString(atomic));
	}

	public void setComparator(String value) {
		comparatorID = value;
	}
}
