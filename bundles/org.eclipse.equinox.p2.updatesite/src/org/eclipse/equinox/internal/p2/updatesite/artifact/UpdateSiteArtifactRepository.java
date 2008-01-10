/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite.artifact;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.generator.features.*;
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.generator.*;
import org.eclipse.equinox.spi.p2.core.repository.AbstractRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.xml.sax.SAXException;

public class UpdateSiteArtifactRepository extends AbstractRepository implements IArtifactRepository {

	private final IArtifactRepository artifactRepository;

	public UpdateSiteArtifactRepository(URL location) {
		super("update site: " + location.toExternalForm(), null, null, location, null, null);
		BundleContext context = Activator.getBundleContext();

		URL localRepositoryURL = null;
		try {
			String stateDirName = Integer.toString(location.toExternalForm().hashCode());
			File bundleData = context.getDataFile(null);
			File stateDir = new File(bundleData, stateDirName);
			localRepositoryURL = stateDir.toURL();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		artifactRepository = initializeArtifactRepository(context, localRepositoryURL, "update site implementation - " + location.toExternalForm());
		try {

			DefaultSiteParser siteParser = new DefaultSiteParser();
			long start = System.currentTimeMillis();
			InputStream is = new BufferedInputStream(location.openStream());
			SiteModel siteModel = siteParser.parse(is);
			System.out.println("Time Fetching Site " + location + " was: " + (System.currentTimeMillis() - start) + " ms");

			// For now we will always refresh the contents. We can do a checksum here.
			artifactRepository.removeAll();

			SiteFeature[] siteFeatures = siteModel.getFeatures();

			FeatureParser featureParser = new FeatureParser();
			System.out.println("Retrieving " + siteFeatures.length + " features");

			Set allSiteArtifacts = new HashSet();

			for (int i = 0; i < siteFeatures.length; i++) {
				SiteFeature siteFeature = siteFeatures[i];
				System.out.println(siteFeature.getFeatureIdentifier());
				URL featureURL = new URL(location, siteFeature.getURLString());

				IArtifactKey featureKey = MetadataGeneratorHelper.createFeatureArtifactKey(siteFeature.getFeatureIdentifier(), siteFeature.getFeatureVersion());
				ArtifactDescriptor featureArtifactDescriptor = new ArtifactDescriptor(featureKey);
				featureArtifactDescriptor.setRepositoryProperty("artifact.reference", featureURL.toExternalForm());
				allSiteArtifacts.add(featureArtifactDescriptor);

				Feature feature = parseFeature(featureParser, featureURL);
				FeatureEntry[] featureEntries = feature.getEntries();
				for (int j = 0; j < featureEntries.length; j++) {
					FeatureEntry entry = featureEntries[j];
					if (entry.isPlugin() && !entry.isRequires()) {
						IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(entry.getId(), entry.getVersion());
						ArtifactDescriptor artifactDescriptor = new ArtifactDescriptor(key);
						URL pluginURL = new URL(location, "plugins/" + entry.getId() + "_" + entry.getVersion() + ".jar");
						artifactDescriptor.setRepositoryProperty("artifact.reference", pluginURL.toExternalForm());
						allSiteArtifacts.add(artifactDescriptor);
					}
				}
			}
			IArtifactDescriptor[] descriptors = (IArtifactDescriptor[]) allSiteArtifacts.toArray(new IArtifactDescriptor[allSiteArtifacts.size()]);
			artifactRepository.addDescriptors(descriptors);

			System.out.println("Time Fetching Site and Features for " + location + " was: " + (System.currentTimeMillis() - start) + " ms");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Feature parseFeature(FeatureParser featureParser, URL featureURL) throws IOException, FileNotFoundException {

		File featureFile = File.createTempFile("feature", ".jar");
		try {
			FileUtils.copyStream(featureURL.openStream(), false, new BufferedOutputStream(new FileOutputStream(featureFile)), true);
			Feature feature = featureParser.parse(featureFile);
			return feature;
		} finally {
			featureFile.delete();
		}
	}

	private IArtifactRepository initializeArtifactRepository(BundleContext context, URL stateDirURL, String repositoryName) {
		ServiceReference reference = context.getServiceReference(IArtifactRepositoryManager.class.getName());
		if (reference == null)
			throw new IllegalStateException("ArtifactRepositoryManager not registered.");

		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException("ArtifactRepositoryManager not registered.");

		IArtifactRepository repository = null;
		try {
			repository = manager.loadRepository(stateDirURL, null);
			if (repository == null) {
				repository = manager.createRepository(stateDirURL, repositoryName, "org.eclipse.equinox.p2.artifact.repository.simpleRepository");
				repository.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
			}
		} finally {
			context.ungetService(reference);
		}

		if (repository == null)
			throw new IllegalStateException("Couldn't create artifact repository for: " + repositoryName);

		return repository;
	}

	public Map getProperties() {
		Map result = new HashMap(artifactRepository.getProperties());
		result.remove(IRepository.PROP_SYSTEM);

		return result;
	}

	public String setProperty(String key, String value) {
		return artifactRepository.setProperty(key, value);
	}

	public void addDescriptor(IArtifactDescriptor descriptor) {
		artifactRepository.addDescriptor(descriptor);
	}

	public boolean contains(IArtifactDescriptor descriptor) {
		return artifactRepository.contains(descriptor);
	}

	public boolean contains(IArtifactKey key) {
		return artifactRepository.contains(key);
	}

	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		return artifactRepository.getArtifact(descriptor, destination, monitor);
	}

	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		return artifactRepository.getArtifactDescriptors(key);
	}

	public IArtifactKey[] getArtifactKeys() {
		return artifactRepository.getArtifactKeys();
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		return artifactRepository.getArtifacts(requests, monitor);
	}

	public OutputStream getOutputStream(IArtifactDescriptor descriptor) {
		return artifactRepository.getOutputStream(descriptor);
	}

	public void removeAll() {
		artifactRepository.removeAll();
	}

	public void removeDescriptor(IArtifactDescriptor descriptor) {
		artifactRepository.removeDescriptor(descriptor);
	}

	public void removeDescriptor(IArtifactKey key) {
		artifactRepository.removeDescriptor(key);
	}

	public void addDescriptors(IArtifactDescriptor[] descriptors) {
		artifactRepository.addDescriptors(descriptors);
	}
}
