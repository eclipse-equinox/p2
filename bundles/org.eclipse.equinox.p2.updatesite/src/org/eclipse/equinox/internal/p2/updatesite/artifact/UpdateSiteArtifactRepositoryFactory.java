/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.internal.p2.updatesite.UpdateSite;
import org.eclipse.equinox.internal.p2.updatesite.metadata.Messages;
import org.eclipse.equinox.internal.p2.updatesite.metadata.UpdateSiteMetadataRepositoryFactory;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.ArtifactRepositoryFactory;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.osgi.util.NLS;

public class UpdateSiteArtifactRepositoryFactory extends ArtifactRepositoryFactory {

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.ArtifactRepositoryFactory#create(java.net.URL, java.lang.String, java.lang.String, java.util.Map)
	 */
	public IArtifactRepository create(URI location, String name, String type, Map properties) {
		return null;
	}

	private static final String PROP_ARTIFACT_REFERENCE = "artifact.reference"; //$NON-NLS-1$
	private static final String PROP_FORCE_THREADING = "eclipse.p2.force.threading"; //$NON-NLS-1$
	private static final String PROP_FORMAT_PACKED = "packed"; //$NON-NLS-1$
	private static final String PROP_SITE_CHECKSUM = "site.checksum"; //$NON-NLS-1$
	private static final String PROTOCOL_FILE = "file"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.ArtifactRepositoryFactory#load(java.net.URL, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IArtifactRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
		//return null if the caller wanted a modifiable repo
		if ((flags & IRepositoryManager.REPOSITORY_HINT_MODIFIABLE) > 0) {
			return null;
		}
		IArtifactRepository repository = loadRepository(location, monitor);
		try {
			initializeRepository(repository, location, monitor);
		} catch (Exception e) {
			resetCache(repository);
			if (e instanceof ProvisionException)
				throw (ProvisionException) e;
			if (e instanceof OperationCanceledException)
				throw (OperationCanceledException) e;
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Unexpected_exception, location.toString()), e));
		}
		return new UpdateSiteArtifactRepository(location, repository);
	}

	private void resetCache(IArtifactRepository repository) {
		repository.setProperty(PROP_SITE_CHECKSUM, "0"); //$NON-NLS-1$
		repository.removeAll();
	}

	public IArtifactRepository loadRepository(URI location, IProgressMonitor monitor) {
		URI localRepositoryURL = UpdateSiteMetadataRepositoryFactory.getLocalRepositoryLocation(location);
		SimpleArtifactRepositoryFactory factory = new SimpleArtifactRepositoryFactory();
		try {
			return factory.load(localRepositoryURL, 0, monitor);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		String repositoryName = "update site: " + location; //$NON-NLS-1$
		return factory.create(localRepositoryURL, repositoryName, null, null);
	}

	public void initializeRepository(IArtifactRepository repository, URI location, IProgressMonitor monitor) throws ProvisionException {
		UpdateSite updateSite = UpdateSite.load(location, monitor);
		String savedChecksum = (String) repository.getProperties().get(PROP_SITE_CHECKSUM);
		if (savedChecksum != null && savedChecksum.equals(updateSite.getChecksum()))
			return;

		if (!location.getScheme().equals(PROTOCOL_FILE))
			repository.setProperty(PROP_FORCE_THREADING, "true"); //$NON-NLS-1$
		repository.setProperty(PROP_SITE_CHECKSUM, updateSite.getChecksum());
		if (updateSite.getSite().getMirrorsURI() != null)
			repository.setProperty(IRepository.PROP_MIRRORS_URL, updateSite.getSite().getMirrorsURI());
		repository.removeAll();
		generateArtifactDescriptors(updateSite, repository, monitor);
	}

	private void generateArtifactDescriptors(UpdateSite updateSite, IArtifactRepository repository, IProgressMonitor monitor) throws ProvisionException {
		final String PACK_EXT = ".pack.gz"; //$NON-NLS-1$
		Feature[] features = updateSite.loadFeatures(monitor);
		Set allSiteArtifacts = new HashSet();
		boolean packSupported = updateSite.getSite().isPack200Supported();
		for (int i = 0; i < features.length; i++) {
			Feature feature = features[i];
			IArtifactKey featureKey = FeaturesAction.createFeatureArtifactKey(feature.getId(), feature.getVersion());
			SimpleArtifactDescriptor featureArtifactDescriptor = new SimpleArtifactDescriptor(featureKey);
			URI featureURL = updateSite.getFeatureURI(feature.getId(), feature.getVersion());
			featureArtifactDescriptor.setRepositoryProperty(PROP_ARTIFACT_REFERENCE, featureURL.toString());
			allSiteArtifacts.add(featureArtifactDescriptor);

			if (packSupported) {
				// Update site supports pack200, create a packed descriptor
				featureArtifactDescriptor = new SimpleArtifactDescriptor(featureKey);
				featureURL = updateSite.getFeatureURI(feature.getId(), feature.getVersion());
				featureArtifactDescriptor.setRepositoryProperty(PROP_ARTIFACT_REFERENCE, featureURL.toString() + PACK_EXT);
				ProcessingStepDescriptor[] steps = new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)}; //$NON-NLS-1$
				featureArtifactDescriptor.setProcessingSteps(steps);
				featureArtifactDescriptor.setProperty(IArtifactDescriptor.FORMAT, PROP_FORMAT_PACKED);
				allSiteArtifacts.add(featureArtifactDescriptor);
			}

			FeatureEntry[] featureEntries = feature.getEntries();
			for (int j = 0; j < featureEntries.length; j++) {
				FeatureEntry entry = featureEntries[j];
				if (entry.isPlugin() && !entry.isRequires()) {
					IArtifactKey key = BundlesAction.createBundleArtifactKey(entry.getId(), entry.getVersion());
					SimpleArtifactDescriptor artifactDescriptor = new SimpleArtifactDescriptor(key);
					URI pluginURL = updateSite.getPluginURI(entry);
					artifactDescriptor.setRepositoryProperty(PROP_ARTIFACT_REFERENCE, pluginURL.toString());
					allSiteArtifacts.add(artifactDescriptor);

					if (packSupported) {
						// Update site supports pack200, create a packed descriptor
						key = BundlesAction.createBundleArtifactKey(entry.getId(), entry.getVersion());
						artifactDescriptor = new SimpleArtifactDescriptor(key);
						pluginURL = updateSite.getPluginURI(entry);
						artifactDescriptor.setRepositoryProperty(PROP_ARTIFACT_REFERENCE, pluginURL.toString() + PACK_EXT);
						ProcessingStepDescriptor[] steps = new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)}; //$NON-NLS-1$
						artifactDescriptor.setProcessingSteps(steps);
						artifactDescriptor.setProperty(IArtifactDescriptor.FORMAT, PROP_FORMAT_PACKED);
						allSiteArtifacts.add(artifactDescriptor);
					}
				}
			}
		}

		IArtifactDescriptor[] descriptors = (IArtifactDescriptor[]) allSiteArtifacts.toArray(new IArtifactDescriptor[allSiteArtifacts.size()]);
		repository.addDescriptors(descriptors);
	}
}
