/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *     Sonatype, Inc. - transport split
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite.artifact;

import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.p2.updatesite.*;
import org.eclipse.equinox.internal.p2.updatesite.metadata.UpdateSiteMetadataRepositoryFactory;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;

public class UpdateSiteArtifactRepositoryFactory extends ArtifactRepositoryFactory {

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.ArtifactRepositoryFactory#create(java.net.URL, java.lang.String, java.lang.String, java.util.Map)
	 */
	public IArtifactRepository create(URI location, String name, String type, Map<String, String> properties) {
		return null;
	}

	private static final String PROP_ARTIFACT_REFERENCE = "artifact.reference"; //$NON-NLS-1$
	private static final String PROP_FORCE_THREADING = "eclipse.p2.force.threading"; //$NON-NLS-1$
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
		if (!isURL(location)) {
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

	private static boolean isURL(URI location) {
		try {
			new URL(location.toASCIIString());
		} catch (MalformedURLException e) {
			return false;
		}
		return true;
	}

	private void resetCache(IArtifactRepository repository) {
		repository.setProperty(PROP_SITE_CHECKSUM, "0"); //$NON-NLS-1$
		repository.removeAll();
	}

	public IArtifactRepository loadRepository(URI location, IProgressMonitor monitor) {
		URI localRepositoryURL = UpdateSiteMetadataRepositoryFactory.getLocalRepositoryLocation(location);
		SimpleArtifactRepositoryFactory factory = new SimpleArtifactRepositoryFactory();
		factory.setAgent(getAgent());
		try {
			return factory.load(localRepositoryURL, 0, monitor);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		String repositoryName = "update site: " + location; //$NON-NLS-1$
		return factory.create(localRepositoryURL, repositoryName, null, null);
	}

	public void initializeRepository(IArtifactRepository repository, URI location, IProgressMonitor monitor) throws ProvisionException {
		UpdateSite updateSite = UpdateSite.load(location, (Transport) getAgent().getService(Transport.SERVICE_NAME), monitor);
		String savedChecksum = repository.getProperties().get(PROP_SITE_CHECKSUM);
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
		Set<IArtifactDescriptor> allSiteArtifacts = new HashSet<IArtifactDescriptor>();
		boolean packSupported = updateSite.getSite().isPack200Supported();
		{
			Feature[] features = updateSite.loadFeatures(monitor);
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
					IProcessingStepDescriptor[] steps = new IProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)}; //$NON-NLS-1$
					featureArtifactDescriptor.setProcessingSteps(steps);
					featureArtifactDescriptor.setProperty(IArtifactDescriptor.FORMAT, IArtifactDescriptor.FORMAT_PACKED);
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
							IProcessingStepDescriptor[] steps = new IProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)}; //$NON-NLS-1$
							artifactDescriptor.setProcessingSteps(steps);
							artifactDescriptor.setProperty(IArtifactDescriptor.FORMAT, IArtifactDescriptor.FORMAT_PACKED);
							allSiteArtifacts.add(artifactDescriptor);
						}
					}
				}
			}
		}
		{
			BundleDescription[] bundles = updateSite.loadBundles(monitor);
			for (int i = 0; i < bundles.length; i++) {
				BundleDescription bundle = bundles[i];
				IArtifactKey bundleKey = BundlesAction.createBundleArtifactKey(bundle.getSymbolicName(), bundle.getVersion().toString());
				SimpleArtifactDescriptor bundleArtifactDescriptor = new SimpleArtifactDescriptor(bundleKey);
				URI bundleURI = updateSite.getBundleURI(bundle.getSymbolicName(), bundle.getVersion().toString());
				bundleArtifactDescriptor.setRepositoryProperty(PROP_ARTIFACT_REFERENCE, bundleURI.toString());
				allSiteArtifacts.add(bundleArtifactDescriptor);

				if (packSupported) {
					// Update site supports pack200, create a packed descriptor
					bundleArtifactDescriptor = new SimpleArtifactDescriptor(bundleKey);
					bundleURI = updateSite.getBundleURI(bundle.getSymbolicName(), bundle.getVersion().toString());
					bundleArtifactDescriptor.setRepositoryProperty(PROP_ARTIFACT_REFERENCE, bundleURI.toString() + PACK_EXT);
					IProcessingStepDescriptor[] steps = new IProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)}; //$NON-NLS-1$
					bundleArtifactDescriptor.setProcessingSteps(steps);
					bundleArtifactDescriptor.setProperty(IArtifactDescriptor.FORMAT, IArtifactDescriptor.FORMAT_PACKED);
					allSiteArtifacts.add(bundleArtifactDescriptor);
				}
			}
		}
		IArtifactDescriptor[] descriptors = allSiteArtifacts.toArray(new IArtifactDescriptor[allSiteArtifacts.size()]);
		repository.addDescriptors(descriptors);
	}
}
