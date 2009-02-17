package org.eclipse.equinox.p2.internal.repository.tools;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;

public abstract class AbstractApplication {

	protected List sourceArtifactRepositories = new ArrayList();
	protected List sourceMetadataRepositories = new ArrayList();
	protected List artifactReposToRemove = new ArrayList();
	protected List metadataReposToRemove = new ArrayList();
	protected List sourceIUs = new ArrayList();
	private List destinationRepos = new ArrayList();

	protected IArtifactRepository destinationArtifactRepository = null;
	protected IMetadataRepository destinationMetadataRepository = null;

	private CompositeMetadataRepository compositeMetadataRepository = null;
	private CompositeArtifactRepository compositeArtifactRepository = null;

	public void addSourceMetadataRepository(String location) {
		URI uri = Activator.getURI(location);
		if (uri != null)
			sourceMetadataRepositories.add(uri);
	}

	public void addSourceMetadataRepository(URI location) {
		if (location != null)
			sourceMetadataRepositories.add(location);
	}

	public List getSourceMetadataRepositories() {
		return sourceMetadataRepositories;
	}

	public void addSourceArtifactRepository(String location) {
		URI uri = Activator.getURI(location);
		if (uri != null)
			sourceArtifactRepositories.add(uri);
	}

	public void addSourceArtifactRepository(URI location) {
		if (location != null)
			sourceArtifactRepositories.add(location);
	}

	public void setSourceIUs(List ius) {
		sourceIUs = ius;
	}

	protected void finalizeRepositories() throws ProvisionException {
		IArtifactRepositoryManager artifactRepositoryManager = Activator.getArtifactRepositoryManager();
		for (Iterator iter = artifactReposToRemove.iterator(); iter.hasNext();)
			artifactRepositoryManager.removeRepository((URI) iter.next());
		IMetadataRepositoryManager metadataRepositoryManager = Activator.getMetadataRepositoryManager();
		for (Iterator iter = metadataReposToRemove.iterator(); iter.hasNext();)
			metadataRepositoryManager.removeRepository((URI) iter.next());
	}

	public void initializeRepos(IProgressMonitor progress) throws ProvisionException {
		IArtifactRepositoryManager artifactRepositoryManager = Activator.getArtifactRepositoryManager();
		if (sourceArtifactRepositories != null && !sourceArtifactRepositories.isEmpty()) {
			for (Iterator iter = sourceArtifactRepositories.iterator(); iter.hasNext();) {
				URI repoLocation = (URI) iter.next();
				if (!artifactRepositoryManager.contains(repoLocation))
					artifactReposToRemove.add(repoLocation);
				artifactRepositoryManager.loadRepository(repoLocation, progress);
			}
		}

		IMetadataRepositoryManager metadataRepositoryManager = Activator.getMetadataRepositoryManager();
		if (sourceArtifactRepositories != null && !sourceArtifactRepositories.isEmpty()) {
			for (Iterator iter = sourceArtifactRepositories.iterator(); iter.hasNext();) {
				URI repoLocation = (URI) iter.next();
				if (!metadataRepositoryManager.contains(repoLocation))
					metadataReposToRemove.add(repoLocation);
				metadataRepositoryManager.loadRepository(repoLocation, progress);
			}
		}

		processDestinationRepos(artifactRepositoryManager, metadataRepositoryManager);

	}

	private void processDestinationRepos(IArtifactRepositoryManager artifactRepositoryManager, IMetadataRepositoryManager metadataRepositoryManager) throws ProvisionException {
		if (destinationRepos.size() != 2) {
			throw new ProvisionException("Too many or too few destination repositories.");
		}
		RepositoryDescriptor artifactRepoDescriptor = ((RepositoryDescriptor) destinationRepos.get(0)).getKind() == IRepository.TYPE_ARTIFACT ? ((RepositoryDescriptor) destinationRepos.get(0)) : ((RepositoryDescriptor) destinationRepos.get(1));
		RepositoryDescriptor metadataRepoDescriptor = ((RepositoryDescriptor) destinationRepos.get(0)).getKind() == IRepository.TYPE_METADATA ? ((RepositoryDescriptor) destinationRepos.get(0)) : ((RepositoryDescriptor) destinationRepos.get(1));
		destinationArtifactRepository = initializeDestination(artifactRepoDescriptor, artifactRepositoryManager);
		destinationMetadataRepository = initializeDestination(metadataRepoDescriptor, metadataRepositoryManager);
	}

	private IMetadataRepository initializeDestination(RepositoryDescriptor toInit, IMetadataRepositoryManager mgr) throws ProvisionException {
		try {
			if (mgr.contains(toInit.getRepoLocation()))
				metadataReposToRemove.add(toInit.getRepoLocation());
			IMetadataRepository repository = mgr.loadRepository(toInit.getRepoLocation(), null);
			if (!repository.isModifiable())
				throw new IllegalArgumentException(NLS.bind(org.eclipse.equinox.p2.internal.repository.tools.Messages.exception_destinationNotModifiable, toInit.getRepoLocation().toString()));
			if (toInit.getName() != null)
				repository.setName(toInit.getName());
			if (!toInit.isAppend())
				repository.removeAll();
			return repository;
		} catch (ProvisionException e) {
			//fall through and create a new repository below
		}
		IMetadataRepository source = null;
		try {
			if (toInit.getFormat() != null)
				source = mgr.loadRepository(URIUtil.fromString(toInit.getFormat()), null);
		} catch (ProvisionException e) {
			//Ignore.
		} catch (URISyntaxException e) {
			//Ignore
		}
		//This code assumes source has been successfully loaded before this point
		//No existing repository; create a new repository at destinationLocation but with source's attributes.
		return mgr.createRepository(toInit.getRepoLocation(), toInit.getName() != null ? toInit.getName() : (source != null ? source.getName() : toInit.getRepoLocation().toString()), IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, source != null ? source.getProperties() : null);
	}

	private IArtifactRepository initializeDestination(RepositoryDescriptor toInit, IArtifactRepositoryManager mgr) throws ProvisionException {
		try {
			if (mgr.contains(toInit.getRepoLocation()))
				artifactReposToRemove.add(toInit.getRepoLocation());
			IArtifactRepository repository = mgr.loadRepository(toInit.getRepoLocation(), null);
			if (!repository.isModifiable())
				throw new IllegalArgumentException(NLS.bind(org.eclipse.equinox.p2.internal.repository.tools.Messages.exception_destinationNotModifiable, toInit.getRepoLocation().toString()));
			if (toInit.getName() != null)
				repository.setName(toInit.getName());
			if (!toInit.isAppend())
				repository.removeAll();
			return repository;
		} catch (ProvisionException e) {
			//fall through and create a new repository below
		}
		IArtifactRepository source = null;
		try {
			if (toInit.getFormat() != null)
				source = mgr.loadRepository(URIUtil.fromString(toInit.getFormat()), null);
		} catch (ProvisionException e) {
			//Ignore.
		} catch (URISyntaxException e) {
			//Ignore
		}
		//This code assumes source has been successfully loaded before this point
		//No existing repository; create a new repository at destinationLocation but with source's attributes.
		// TODO for now create a Simple repo by default.
		return mgr.createRepository(toInit.getRepoLocation(), toInit.getName() != null ? toInit.getName() : (source != null ? source.getName() : toInit.getRepoLocation().toString()), IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, source != null ? source.getProperties() : null);
	}

	public IMetadataRepository getCompositeMetadataRepository() {
		if (compositeMetadataRepository == null) {
			try {
				compositeMetadataRepository = new CompositeMetadataRepository(new URI("memory:/composite"), "parent metadata repo", null);//$NON-NLS-1$ //$NON-NLS-2$
			} catch (URISyntaxException e) {
				//Can't happen
			}
			for (Iterator iter = sourceMetadataRepositories.iterator(); iter.hasNext();) {
				compositeMetadataRepository.addChild((URI) iter.next());
			}
		}
		return compositeMetadataRepository;
	}

	public IArtifactRepository getCompositeArtifactRepository() {
		if (compositeArtifactRepository == null) {
			try {
				compositeArtifactRepository = new CompositeArtifactRepository(new URI("memory:/composite"), "parent metadata repo", null);//$NON-NLS-1$ //$NON-NLS-2$
			} catch (URISyntaxException e) {
				//Can't happen
			}
			for (Iterator iter = sourceArtifactRepositories.iterator(); iter.hasNext();) {
				compositeArtifactRepository.addChild((URI) iter.next());
			}
		}
		return compositeArtifactRepository;
	}

	public abstract IStatus run(IProgressMonitor monitor) throws ProvisionException;

	public void addDestination(RepositoryDescriptor descriptor) {
		destinationRepos.add(descriptor);
	}
}