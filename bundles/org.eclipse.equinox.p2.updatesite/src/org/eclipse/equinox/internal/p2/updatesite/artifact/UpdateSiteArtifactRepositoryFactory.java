/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite.artifact;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.updatesite.UpdateSite;
import org.eclipse.equinox.internal.p2.updatesite.metadata.UpdateSiteMetadataRepositoryFactory;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.IArtifactRepositoryFactory;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class UpdateSiteArtifactRepositoryFactory implements IArtifactRepositoryFactory {

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.IArtifactRepositoryFactory#create(java.net.URL, java.lang.String, java.lang.String, java.util.Map)
	 */
	public IArtifactRepository create(URI location, String name, String type, Map properties) {
		return null;
	}

	private static final String PROP_ARTIFACT_REFERENCE = "artifact.reference"; //$NON-NLS-1$
	private static final String PROP_FORCE_THREADING = "eclipse.p2.force.threading"; //$NON-NLS-1$
	private static final String PROP_SITE_CHECKSUM = "site.checksum"; //$NON-NLS-1$
	private static final String PROTOCOL_FILE = "file"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.IArtifactRepositoryFactory#load(java.net.URL, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IArtifactRepository load(URI location, IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepository repository = loadRepository(location, monitor);
		initializeRepository(repository, location, monitor);
		return repository;
	}

	public IArtifactRepository loadRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
		URI localRepositoryURL = UpdateSiteMetadataRepositoryFactory.getLocalRepositoryLocation(location);
		SimpleArtifactRepositoryFactory factory = new SimpleArtifactRepositoryFactory();
		try {
			IArtifactRepository repository = factory.load(localRepositoryURL, monitor);
			if (!repository.getProperties().get(IRepository.PROP_SYSTEM).equals(Boolean.TRUE.toString()))
				repository.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
			return repository;
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		String repositoryName = "update site: " + location; //$NON-NLS-1$
		Properties props = new Properties();
		props.put(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
		return factory.create(localRepositoryURL, repositoryName, null, props);
	}

	public void initializeRepository(IArtifactRepository repository, URI location, IProgressMonitor monitor) throws ProvisionException {
		UpdateSite updateSite = UpdateSite.load(location, monitor);
		String savedChecksum = (String) repository.getProperties().get(PROP_SITE_CHECKSUM);
		if (savedChecksum != null && savedChecksum.equals(updateSite.getChecksum()))
			return;

		if (!location.getScheme().equals(PROTOCOL_FILE))
			repository.setProperty(PROP_FORCE_THREADING, "true"); //$NON-NLS-1$
		repository.setProperty(PROP_SITE_CHECKSUM, updateSite.getChecksum());
		repository.removeAll();
		generateArtifactDescriptors(updateSite, repository, monitor);
	}

	private void generateArtifactDescriptors(UpdateSite updateSite, IArtifactRepository repository, IProgressMonitor monitor) throws ProvisionException {
		Feature[] features = updateSite.loadFeatures(monitor);
		Set allSiteArtifacts = new HashSet();
		for (int i = 0; i < features.length; i++) {
			Feature feature = features[i];
			IArtifactKey featureKey = FeaturesAction.createFeatureArtifactKey(feature.getId(), feature.getVersion());
			ArtifactDescriptor featureArtifactDescriptor = new ArtifactDescriptor(featureKey);
			URI featureURL = updateSite.getFeatureURI(feature.getId(), feature.getVersion());
			featureArtifactDescriptor.setRepositoryProperty(PROP_ARTIFACT_REFERENCE, featureURL.toString());
			allSiteArtifacts.add(featureArtifactDescriptor);

			FeatureEntry[] featureEntries = feature.getEntries();
			for (int j = 0; j < featureEntries.length; j++) {
				FeatureEntry entry = featureEntries[j];
				if (entry.isPlugin() && !entry.isRequires()) {
					IArtifactKey key = PublisherHelper.createBundleArtifactKey(entry.getId(), entry.getVersion());
					ArtifactDescriptor artifactDescriptor = new ArtifactDescriptor(key);
					URI pluginURL = updateSite.getPluginURI(entry);
					artifactDescriptor.setRepositoryProperty(PROP_ARTIFACT_REFERENCE, pluginURL.toString());
					allSiteArtifacts.add(artifactDescriptor);
				}
			}
		}

		IArtifactDescriptor[] descriptors = (IArtifactDescriptor[]) allSiteArtifacts.toArray(new IArtifactDescriptor[allSiteArtifacts.size()]);
		repository.addDescriptors(descriptors);
	}
}
