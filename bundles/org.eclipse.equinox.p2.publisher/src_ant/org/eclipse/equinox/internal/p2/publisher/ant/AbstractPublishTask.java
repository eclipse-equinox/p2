/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.ant;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.publisher.*;

public abstract class AbstractPublishTask extends Task {
	protected static final String ANT_PROPERTY_PREFIX = "${"; //$NON-NLS-1$

	protected boolean compress = false;
	protected boolean reusePackedFiles = false;
	protected boolean append = true;
	protected boolean publish = true;
	protected URI metadataLocation;
	protected String metadataRepoName;
	protected URI artifactLocation;
	protected String artifactRepoName;
	protected PublisherInfo provider = null;

	protected void initializeRepositories(PublisherInfo info) throws ProvisionException {
		info.setArtifactRepository(Publisher.createArtifactRepository(artifactLocation, artifactRepoName, append, compress, reusePackedFiles));
		info.setMetadataRepository(Publisher.createMetadataRepository(metadataLocation, metadataRepoName, append, compress));
	}

	protected PublisherInfo getInfo() {
		if (provider == null)
			provider = new PublisherInfo();

		if (publish)
			provider.setArtifactOptions(provider.getArtifactOptions() | IPublisherInfo.A_PUBLISH);
		return provider;
	}

	public void setCompress(String value) {
		compress = Boolean.valueOf(value).booleanValue();
	}

	public void setReusePackedFiles(String value) {
		reusePackedFiles = Boolean.valueOf(value).booleanValue();
	}

	public void setAppend(String value) {
		append = Boolean.valueOf(value).booleanValue();
	}

	public void setPublishArtifacts(String value) {
		publish = Boolean.valueOf(value).booleanValue();
	}

	public void setArtifactRepository(String location) {
		try {
			artifactLocation = URIUtil.fromString(location);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Artifact repository location (" + location + ") must be a URL."); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	public void setArtifactRepositoryName(String value) {
		artifactRepoName = value;
	}

	public void setMetadataRepository(String location) {
		try {
			metadataLocation = URIUtil.fromString(location);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Metadata repository location (" + location + ") must be a URL."); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void setMetadataRepositoryName(String value) {
		metadataRepoName = value;
	}

	public void setRepository(String location) {
		setArtifactRepository(location);
		setMetadataRepository(location);
	}

	public void setRepositoryName(String name) {
		setArtifactRepositoryName(name);
		setMetadataRepositoryName(name);
	}
}
