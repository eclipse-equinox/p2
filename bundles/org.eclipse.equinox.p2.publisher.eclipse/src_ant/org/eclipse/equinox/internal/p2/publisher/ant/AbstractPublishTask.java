/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *     IBM - Initial API and implementation
 *     Red Hat Inc. - Bug 460967
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.ant;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.internal.p2.publisher.Messages;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.publishing.Activator;

public abstract class AbstractPublishTask extends Task {
	protected static final String ANT_PROPERTY_PREFIX = "${"; //$NON-NLS-1$

	/**
	 * Support nested repository elements that looking something like
	 *    <repo location="file:/foo" metadata="true" artifact="true" />
	 * Both metadata and artifact are optional:
	 *  1) if neither are set, the repo is used for both metadata and artifacts
	 *  2) if only one is true, the repo is that type and not the other 
	 */
	static public class RepoEntry {
		private URI repoLocation;
		private Boolean metadata = null;
		private Boolean artifact = null;

		/**
		 * If not set, default is true if we aren't set as an artifact repo 
		 */
		public boolean isMetadataRepository() {
			if (metadata != null)
				return metadata.booleanValue();
			return !Boolean.TRUE.equals(artifact);
		}

		/**
		 * If not set, default is true if we aren't set as a metadata repo 
		 */
		public boolean isArtifactRepository() {
			if (artifact != null)
				return artifact.booleanValue();
			return !Boolean.TRUE.equals(metadata);
		}

		public URI getRepositoryLocation() {
			return repoLocation;
		}

		public void setLocation(String location) {
			try {
				repoLocation = URIUtil.fromString(location);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Repository location (" + location + ") must be a URL."); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		public void setMetadata(boolean metadata) {
			this.metadata = Boolean.valueOf(metadata);
		}

		public void setArtifact(boolean artifact) {
			this.artifact = Boolean.valueOf(artifact);
		}
	}

	protected boolean compress = false;
	protected boolean reusePackedFiles = false;
	protected boolean append = true;
	protected boolean publish = true;
	protected String source = null;
	protected URI metadataLocation;
	protected String metadataRepoName;
	protected URI artifactLocation;
	protected String artifactRepoName;
	protected PublisherInfo provider = null;
	protected List<RepoEntry> contextRepositories = new ArrayList<RepoEntry>();

	protected IProvisioningAgent getProvisioningAgent() {
		return ServiceHelper.getService(Activator.getContext(), IProvisioningAgent.class);
	}

	protected IStatus createConfigurationEror(String message) {
		return new Status(IStatus.ERROR, "org.eclipse.equinox.p2.publisher", message); //$NON-NLS-1$
	}

	private boolean isEmpty(IArtifactRepository repo) {
		IQueryResult<IArtifactKey> result = repo.query(QueryUtil.createLimitQuery(ArtifactKeyQuery.ALL_KEYS, 1), null);
		return result.isEmpty();
	}

	protected void initializeRepositories(PublisherInfo info) throws ProvisionException {
		if (artifactLocation != null) {
			IArtifactRepository repo = Publisher.createArtifactRepository(getProvisioningAgent(), artifactLocation, artifactRepoName, compress, reusePackedFiles);
			if (!append && !isEmpty(repo)) {
				File repoLocation = URIUtil.toFile(artifactLocation);
				if (repoLocation != null && source != null) {
					if (repoLocation.isFile())
						repoLocation = repoLocation.getParentFile();
					if (repoLocation.equals(new File(source)))
						throw new IllegalArgumentException(NLS.bind(Messages.exception_artifactRepoNoAppendDestroysInput, URIUtil.toUnencodedString(artifactLocation)));
				}
				repo.removeAll(new NullProgressMonitor());
			}
			info.setArtifactRepository(repo);
		} else if ((info.getArtifactOptions() & IPublisherInfo.A_PUBLISH) > 0)
			throw new ProvisionException(createConfigurationEror(Messages.exception_noArtifactRepo));
		if (metadataLocation == null)
			throw new ProvisionException(createConfigurationEror(Messages.exception_noMetadataRepo));
		info.setMetadataRepository(Publisher.createMetadataRepository(getProvisioningAgent(), metadataLocation, metadataRepoName, append, compress));

		if (contextRepositories.size() > 0) {
			CompositeMetadataRepository contextMetadata = CompositeMetadataRepository.createMemoryComposite(getProvisioningAgent());
			CompositeArtifactRepository contextArtifact = CompositeArtifactRepository.createMemoryComposite(getProvisioningAgent());

			for (RepoEntry entry : contextRepositories) {
				if (contextMetadata != null && entry.isMetadataRepository())
					contextMetadata.addChild(entry.getRepositoryLocation());
				if (contextArtifact != null && entry.isArtifactRepository())
					contextArtifact.addChild(entry.getRepositoryLocation());
			}

			if (contextMetadata != null && contextMetadata.getChildren().size() > 0)
				info.setContextMetadataRepository(contextMetadata);

			if (contextArtifact != null && contextArtifact.getChildren().size() > 0)
				info.setContextArtifactRepository(contextArtifact);
		}
	}

	protected PublisherInfo getInfo() {
		if (provider == null)
			provider = new PublisherInfo();

		if (publish)
			provider.setArtifactOptions(provider.getArtifactOptions() | IPublisherInfo.A_PUBLISH);
		return provider;
	}

	public void setCompress(String value) {
		compress = Boolean.parseBoolean(value);
	}

	public void setReusePackedFiles(String value) {
		reusePackedFiles = Boolean.parseBoolean(value);
	}

	public void setAppend(String value) {
		append = Boolean.parseBoolean(value);
	}

	public void setPublishArtifacts(String value) {
		publish = Boolean.parseBoolean(value);
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

	// nested <contextRepository/> elements
	public void addConfiguredContextRepository(RepoEntry repo) {
		contextRepositories.add(repo);
	}
}
