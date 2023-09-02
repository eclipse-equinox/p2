/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;

public class CompositeRepositoryApplication extends AbstractApplication {

	private List<RepositoryDescriptor> childrenToAdd = new ArrayList<>();
	private List<RepositoryDescriptor> childrenToRemove = new ArrayList<>();
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
	@SuppressWarnings("unchecked")
	public IStatus run(IProgressMonitor monitor) throws ProvisionException {
		try {
			initializeRepos(new NullProgressMonitor());
			// load repository
			ICompositeRepository<IInstallableUnit> metadataRepo = (ICompositeRepository<IInstallableUnit>) destinationMetadataRepository;
			CompositeArtifactRepository artifactRepo = (CompositeArtifactRepository) destinationArtifactRepository;

			if (removeAllChildren) {
				if (artifactRepo != null)
					artifactRepo.removeAllChildren();
				if (metadataRepo != null)
					metadataRepo.removeAllChildren();
			} else {
				// Remove children from the Composite Repositories
				for (RepositoryDescriptor child : childrenToRemove) {
					if (child.isArtifact() && artifactRepo != null)
						artifactRepo.removeChild(child.getOriginalRepoLocation());
					if (child.isMetadata() && metadataRepo != null)
						metadataRepo.removeChild(child.getOriginalRepoLocation());
				}
			}

			// Add children to the Composite Repositories
			for (RepositoryDescriptor child : childrenToAdd) {
				if (child.isArtifact() && artifactRepo != null)
					artifactRepo.addChild(child.getOriginalRepoLocation());
				if (child.isMetadata() && metadataRepo != null)
					metadataRepo.addChild(child.getOriginalRepoLocation());
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

		String repositoryType;
		String defaultName;
		if (mgr instanceof IArtifactRepositoryManager) {
			repositoryType = IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY;
			defaultName = Messages.CompositeRepository_default_artifactRepo_name;
		} else if (mgr instanceof IMetadataRepositoryManager) {
			repositoryType = IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY;
			defaultName = Messages.CompositeRepository_default_metadataRepo_name;
		} else {
			throw new AssertionError("Unsupported repository type: " + mgr.getClass()); //$NON-NLS-1$
		}
		// remove the repo first.
		mgr.removeRepository(toInit.getRepoLocation());

		// first try and load to see if one already exists at that location.
		try {
			IRepository<T> repository = mgr.loadRepository(toInit.getRepoLocation(), null);
			validRepositoryLocation(repository);
			if (!initDestinationRepository(repository, toInit)) {
				throw new ProvisionException(Status.info(
						NLS.bind(Messages.CompositeRepository_composite_repository_exists, toInit.getRepoLocation())));
			}
			return repository;
		} catch (ProvisionException e) {
			// re-throw the exception if we got anything other than "repo not found"
			if (e.getStatus().getCode() != ProvisionException.REPOSITORY_NOT_FOUND) {
				if (e.getCause() instanceof MalformedURLException)
					throw new ProvisionException(
							NLS.bind(Messages.exception_invalidDestination, toInit.getRepoLocation()), e.getCause());
				throw e;
			}
		}

		IRepository<T> source = null;
		try {
			if (toInit.getFormat() != null) {
				source = mgr.loadRepository(toInit.getFormat(), 0, null);
			}
		} catch (ProvisionException e) {
			// Ignore
		}
		// This code assumes source has been successfully loaded before this point
		try {
			// No existing repository; create a new repository at destinationLocation but
			// with source's attributes.
			String name = Optional.ofNullable(toInit.getName()).orElse(source != null ? source.getName() : defaultName);
			IRepository<T> repo = mgr.createRepository(toInit.getRepoLocation(), name, repositoryType,
					source != null ? source.getProperties() : null);
			initRepository(repo, toInit);
			return repo;
		} catch (IllegalStateException e) {
			mgr.removeRepository(toInit.getRepoLocation());
			throw e;
		}
	}

	/*
	 * Determine if the repository is valid for this operation
	 */
	private void validRepositoryLocation(IRepository<?> repository) throws ProvisionException {
		if (repository instanceof ICompositeRepository<?>) {
			// if we have an already existing repository at that location, then throw an error
			// if the user told us to
			if (failOnExists) {
				throw new ProvisionException(NLS.bind(Messages.CompositeRepository_composite_repository_exists, repository.getLocation()));
			}
			RepositoryHelper.validDestinationRepository(repository);
		}
		// we have a non-composite repo at this location. that is ok because we can co-exist.
	}

	/*
	 * Initialize a new repository
	 */
	private void initRepository(IRepository<?> repository, RepositoryDescriptor desc) {
		RepositoryHelper.validDestinationRepository(repository);
		if (desc.isCompressed() && !repository.getProperties().containsKey(IRepository.PROP_COMPRESSED))
			repository.setProperty(IRepository.PROP_COMPRESSED, String.valueOf(true));

		setAtomicLoadingProperty(repository, desc);
	}

	private void setAtomicLoadingProperty(IRepository<?> repository, RepositoryDescriptor desc) {
		// bug 356561: newly created repositories shall be atomic (by default)
		boolean atomic = true;
		if (desc.getAtomic() != null)
			atomic = Boolean.valueOf(desc.getAtomic());
		repository.setProperty(CompositeMetadataRepository.PROP_ATOMIC_LOADING, Boolean.toString(atomic));
	}

	public void setComparator(String value) {
		comparatorID = value;
	}
}
