package org.eclipse.equinox.p2.directorywatcher;

import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;

public class RepositoryUpdatedEvent {

	private final IMetadataRepository metadataRepository;
	private final IArtifactRepository artifactRepository;

	public RepositoryUpdatedEvent(IMetadataRepository metadataRepository, IArtifactRepository artifactRepository) {
		this.metadataRepository = metadataRepository;
		this.artifactRepository = artifactRepository;
	}

	public IMetadataRepository getMetadataRepository() {
		return metadataRepository;
	}

	public IArtifactRepository getArtifactRepository() {
		return artifactRepository;
	}
}
