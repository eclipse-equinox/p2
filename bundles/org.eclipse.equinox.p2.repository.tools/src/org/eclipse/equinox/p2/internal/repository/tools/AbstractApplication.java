/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools;

import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.repository.*;
import org.eclipse.osgi.util.NLS;

public abstract class AbstractApplication {
	protected boolean removeAddedRepositories = true;

	protected List sourceRepositories = new ArrayList(); //List of repository descriptors
	protected List artifactReposToRemove = new ArrayList();
	protected List metadataReposToRemove = new ArrayList();
	protected List sourceIUs = new ArrayList();
	private List destinationRepos = new ArrayList();

	protected IArtifactRepository destinationArtifactRepository = null;
	protected IMetadataRepository destinationMetadataRepository = null;

	private CompositeMetadataRepository compositeMetadataRepository = null;
	private CompositeArtifactRepository compositeArtifactRepository = null;

	public void setSourceIUs(List ius) {
		sourceIUs = ius;
	}

	protected void finalizeRepositories() throws ProvisionException {
		if (removeAddedRepositories) {
			IArtifactRepositoryManager artifactRepositoryManager = Activator.getArtifactRepositoryManager();
			for (Iterator iter = artifactReposToRemove.iterator(); iter.hasNext();)
				artifactRepositoryManager.removeRepository((URI) iter.next());
			IMetadataRepositoryManager metadataRepositoryManager = Activator.getMetadataRepositoryManager();
			for (Iterator iter = metadataReposToRemove.iterator(); iter.hasNext();)
				metadataRepositoryManager.removeRepository((URI) iter.next());
		}
	}

	public void initializeRepos(IProgressMonitor progress) throws ProvisionException {
		IArtifactRepositoryManager artifactRepositoryManager = Activator.getArtifactRepositoryManager();
		IMetadataRepositoryManager metadataRepositoryManager = Activator.getMetadataRepositoryManager();
		URI curLocation = null;
		try {
			for (Iterator iter = sourceRepositories.iterator(); iter.hasNext();) {
				RepositoryDescriptor repo = (RepositoryDescriptor) iter.next();
				curLocation = repo.getRepoLocation();
				if (repo.isBoth()) {
					addRepository(artifactRepositoryManager, curLocation, 0, progress);
					addRepository(metadataRepositoryManager, curLocation, 0, progress);
				} else if (repo.isArtifact())
					addRepository(artifactRepositoryManager, curLocation, 0, progress);
				else if (repo.isMetadata())
					addRepository(metadataRepositoryManager, curLocation, 0, progress);
				else
					throw new ProvisionException(NLS.bind(Messages.unknown_repository_type, repo.getRepoLocation()));
			}
		} catch (ProvisionException e) {
			if (e.getCause() instanceof MalformedURLException) {
				throw new ProvisionException(NLS.bind(Messages.exception_invalidSource, curLocation), e);
			}
			throw e;
		}
		processDestinationRepos(artifactRepositoryManager, metadataRepositoryManager);
	}

	//Helper to add a repository. It takes care of adding the repos to the deletion list and loading it 
	protected IMetadataRepository addRepository(IMetadataRepositoryManager manager, URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
		if (!manager.contains(location))
			metadataReposToRemove.add(location);
		return manager.loadRepository(location, flags, monitor);
	}

	//Helper to add a repository. It takes care of adding the repos to the deletion list and loading it
	protected IArtifactRepository addRepository(IArtifactRepositoryManager manager, URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
		if (!manager.contains(location))
			artifactReposToRemove.add(location);
		return manager.loadRepository(location, flags, monitor);
	}

	private void processDestinationRepos(IArtifactRepositoryManager artifactRepositoryManager, IMetadataRepositoryManager metadataRepositoryManager) throws ProvisionException {
		RepositoryDescriptor artifactRepoDescriptor = null;
		RepositoryDescriptor metadataRepoDescriptor = null;

		Iterator iter = destinationRepos.iterator();
		while (iter.hasNext() && (artifactRepoDescriptor == null || metadataRepoDescriptor == null)) {
			RepositoryDescriptor repo = (RepositoryDescriptor) iter.next();
			if (repo.isArtifact() && artifactRepoDescriptor == null)
				artifactRepoDescriptor = repo;
			if (repo.isMetadata() && metadataRepoDescriptor == null)
				metadataRepoDescriptor = repo;
		}

		if (artifactRepoDescriptor != null)
			destinationArtifactRepository = initializeDestination(artifactRepoDescriptor, artifactRepositoryManager);
		if (metadataRepoDescriptor != null)
			destinationMetadataRepository = initializeDestination(metadataRepoDescriptor, metadataRepositoryManager);

		if (destinationMetadataRepository == null && destinationArtifactRepository == null)
			throw new ProvisionException(Messages.AbstractApplication_no_valid_destinations);
	}

	public IMetadataRepository getDestinationMetadataRepository() {
		return destinationMetadataRepository;
	}

	public IArtifactRepository getDestinationArtifactRepository() {
		return destinationArtifactRepository;
	}

	protected IMetadataRepository initializeDestination(RepositoryDescriptor toInit, IMetadataRepositoryManager mgr) throws ProvisionException {
		try {
			IMetadataRepository repository = addRepository(mgr, toInit.getRepoLocation(), IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, null);
			if (initDestinationRepository(repository, toInit))
				return repository;
		} catch (ProvisionException e) {
			//fall through and create a new repository below
		}

		IMetadataRepository source = null;
		try {
			if (toInit.getFormat() != null)
				source = mgr.loadRepository(toInit.getFormat(), 0, null);
		} catch (ProvisionException e) {
			//Ignore.
		}
		//This code assumes source has been successfully loaded before this point
		//No existing repository; create a new repository at destinationLocation but with source's attributes.
		try {
			IMetadataRepository result = mgr.createRepository(toInit.getRepoLocation(), toInit.getName() != null ? toInit.getName() : (source != null ? source.getName() : toInit.getRepoLocation().toString()), IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, source != null ? source.getProperties() : null);
			if (toInit.isCompressed() && !result.getProperties().containsKey(IRepository.PROP_COMPRESSED))
				result.setProperty(IRepository.PROP_COMPRESSED, "true"); //$NON-NLS-1$
			return (IMetadataRepository) RepositoryHelper.validDestinationRepository(result);
		} catch (UnsupportedOperationException e) {
			throw new ProvisionException(NLS.bind(Messages.exception_invalidDestination, toInit.getRepoLocation()), e.getCause());
		}
	}

	protected IArtifactRepository initializeDestination(RepositoryDescriptor toInit, IArtifactRepositoryManager mgr) throws ProvisionException {
		try {
			IArtifactRepository repository = addRepository(mgr, toInit.getRepoLocation(), IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, null);
			if (initDestinationRepository(repository, toInit))
				return repository;
		} catch (ProvisionException e) {
			//fall through and create a new repository below
		}
		IArtifactRepository source = null;
		try {
			if (toInit.getFormat() != null)
				source = mgr.loadRepository(toInit.getFormat(), 0, null);
		} catch (ProvisionException e) {
			//Ignore.
		}
		//This code assumes source has been successfully loaded before this point
		//No existing repository; create a new repository at destinationLocation but with source's attributes.
		// TODO for now create a Simple repo by default.
		try {
			IArtifactRepository result = mgr.createRepository(toInit.getRepoLocation(), toInit.getName() != null ? toInit.getName() : (source != null ? source.getName() : toInit.getRepoLocation().toString()), IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, source != null ? source.getProperties() : null);
			if (toInit.isCompressed() && !result.getProperties().containsKey(IRepository.PROP_COMPRESSED))
				result.setProperty(IRepository.PROP_COMPRESSED, "true"); //$NON-NLS-1$
			return (IArtifactRepository) RepositoryHelper.validDestinationRepository(result);
		} catch (UnsupportedOperationException e) {
			throw new ProvisionException(NLS.bind(Messages.exception_invalidDestination, toInit.getRepoLocation()), e.getCause());
		}
	}

	protected boolean initDestinationRepository(IRepository repository, RepositoryDescriptor descriptor) {
		if (repository != null && repository.isModifiable()) {
			if (descriptor.getName() != null)
				repository.setName(descriptor.getName());
			if (repository instanceof ICompositeRepository && !descriptor.isAppend())
				((ICompositeRepository) repository).removeAllChildren();
			else if (repository instanceof IMetadataRepository && !descriptor.isAppend())
				((IMetadataRepository) repository).removeAll();
			else if (repository instanceof IArtifactRepository && !descriptor.isAppend())
				((IArtifactRepository) repository).removeAll();
			return true;
		}
		return false;
	}

	public IMetadataRepository getCompositeMetadataRepository() {
		if (compositeMetadataRepository == null) {
			try {
				compositeMetadataRepository = new CompositeMetadataRepository(new URI("memory:/composite"), "parent metadata repo", null);//$NON-NLS-1$ //$NON-NLS-2$
			} catch (URISyntaxException e) {
				//Can't happen
			}
			for (Iterator iter = sourceRepositories.iterator(); iter.hasNext();) {
				RepositoryDescriptor repo = (RepositoryDescriptor) iter.next();
				if (repo.isMetadata())
					compositeMetadataRepository.addChild(repo.getRepoLocation());
			}
		}
		return compositeMetadataRepository;
	}

	public IArtifactRepository getCompositeArtifactRepository() {
		if (compositeArtifactRepository == null) {
			try {
				compositeArtifactRepository = new CompositeArtifactRepository(new URI("memory:/composite"), "parent artifact repo", null);//$NON-NLS-1$ //$NON-NLS-2$
			} catch (URISyntaxException e) {
				//Can't happen
			}
			for (Iterator iter = sourceRepositories.iterator(); iter.hasNext();) {
				RepositoryDescriptor repo = (RepositoryDescriptor) iter.next();
				if (repo.isArtifact())
					compositeArtifactRepository.addChild(repo.getRepoLocation());
			}
		}
		return compositeArtifactRepository;
	}

	public boolean hasArtifactSources() {
		return ((ICompositeRepository) getCompositeArtifactRepository()).getChildren().size() > 0;
	}

	public boolean hasMetadataSources() {
		return ((ICompositeRepository) getCompositeMetadataRepository()).getChildren().size() > 0;
	}

	public abstract IStatus run(IProgressMonitor monitor) throws ProvisionException;

	public void addDestination(RepositoryDescriptor descriptor) {
		destinationRepos.add(descriptor);
	}

	public void addSource(RepositoryDescriptor repo) {
		sourceRepositories.add(repo);
	}
}