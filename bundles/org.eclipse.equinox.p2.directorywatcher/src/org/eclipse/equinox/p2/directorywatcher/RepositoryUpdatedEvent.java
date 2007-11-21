/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
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
