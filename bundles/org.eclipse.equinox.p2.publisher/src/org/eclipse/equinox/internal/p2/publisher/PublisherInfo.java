/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher;

import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;

public class PublisherInfo implements IPublisherInfo {

	private boolean publishArtifacts = false;
	private boolean publishArtifactRepository = false;
	private IMetadataRepository metadataRepository;
	private IArtifactRepository artifactRepository;

	public IArtifactRepository getArtifactRepository() {
		return artifactRepository;
	}

	public IMetadataRepository getMetadataRepository() {
		return metadataRepository;
	}

	public boolean publishArtifactRepository() {
		return publishArtifactRepository;
	}

	public boolean publishArtifacts() {
		return publishArtifacts;
	}

	public void setArtifactRepository(IArtifactRepository value) {
		artifactRepository = value;
	}

	public void setMetadataRepository(IMetadataRepository value) {
		metadataRepository = value;
	}

	public void setPublishArtifactRepository(boolean value) {
		publishArtifactRepository = value;
	}

	public void setPublishArtifacts(boolean value) {
		publishArtifacts = value;
	}

	public String getSummary() {
		return "some text that describes this setup";
	}

}
