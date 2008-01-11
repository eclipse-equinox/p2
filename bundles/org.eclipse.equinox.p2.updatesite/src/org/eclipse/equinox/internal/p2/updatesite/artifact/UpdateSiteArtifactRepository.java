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
import java.util.zip.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.metadata.generator.features.*;
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.generator.*;
import org.eclipse.equinox.spi.p2.core.repository.AbstractRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.xml.sax.SAXException;

public class UpdateSiteArtifactRepository extends AbstractRepository implements IArtifactRepository {

	private final IArtifactRepository artifactRepository;

	public UpdateSiteArtifactRepository(URL location, IProgressMonitor monitor) {
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
			Checksum checksum = new CRC32();
			InputStream is = new CheckedInputStream(new BufferedInputStream(location.openStream()), checksum);
			SiteModel siteModel = siteParser.parse(is);
			System.out.println("Time Fetching Artifact Site " + location + " was: " + (System.currentTimeMillis() - start) + " ms");

			String savedChecksum = (String) artifactRepository.getProperties().get("site.checksum");
			String checksumString = Long.toString(checksum.getValue());
			if (savedChecksum != null && savedChecksum.equals(checksumString))
				return;

			artifactRepository.setProperty("site.checksum", checksumString);
			artifactRepository.removeAll();

			SiteFeature[] siteFeatures = siteModel.getFeatures();

			Feature[] features = loadFeaturesFromDigest(location, siteModel);
			if (features == null) {
				features = loadFeaturesFromSiteFeatures(location, siteFeatures);
			}

			Set allSiteArtifacts = new HashSet();
			for (int i = 0; i < features.length; i++) {
				Feature feature = features[i];
				IArtifactKey featureKey = MetadataGeneratorHelper.createFeatureArtifactKey(feature.getId(), feature.getVersion());
				ArtifactDescriptor featureArtifactDescriptor = new ArtifactDescriptor(featureKey);
				URL featureURL = new URL(location, "features/" + feature.getId() + "_" + feature.getVersion() + ".jar");
				featureArtifactDescriptor.setRepositoryProperty("artifact.reference", featureURL.toExternalForm());
				allSiteArtifacts.add(featureArtifactDescriptor);

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

			System.out.println("Time Fetching Artifact Site and Features for " + location + " was: " + (System.currentTimeMillis() - start) + " ms");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Feature[] loadFeaturesFromDigest(URL location, SiteModel siteModel) {
		try {
			URL digestURL = new URL(location, "digest.zip");
			File digestFile = File.createTempFile("digest", ".zip");
			try {
				FileUtils.copyStream(digestURL.openStream(), false, new BufferedOutputStream(new FileOutputStream(digestFile)), true);
				Feature[] features = new DigestParser().parse(digestFile);
				return features;
			} finally {
				digestFile.delete();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace(); // unexpected
		} catch (IOException e) {
			// TODO log this
		}
		return null;
	}

	private Feature[] loadFeaturesFromSiteFeatures(URL location, SiteFeature[] siteFeatures) {
		FeatureParser featureParser = new FeatureParser();
		Map featuresMap = new HashMap();
		for (int i = 0; i < siteFeatures.length; i++) {
			SiteFeature siteFeature = siteFeatures[i];
			String key = siteFeature.getFeatureIdentifier() + "_" + siteFeature.getFeatureVersion();
			if (featuresMap.containsKey(key))
				continue;

			try {
				URL featureURL = new URL(location, siteFeature.getURLString());
				Feature feature = parseFeature(featureParser, featureURL);
				featuresMap.put(key, feature);
				loadIncludedFeatures(feature, featureParser, featuresMap);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return (Feature[]) featuresMap.values().toArray(new Feature[featuresMap.size()]);
	}

	private void loadIncludedFeatures(Feature feature, FeatureParser featureParser, Map featuresMap) {
		FeatureEntry[] featureEntries = feature.getEntries();
		for (int i = 0; i < featureEntries.length; i++) {
			FeatureEntry entry = featureEntries[i];
			if (entry.isRequires() || entry.isPlugin())
				continue;

			String key = entry.getId() + "_" + entry.getVersion();
			if (featuresMap.containsKey(key))
				continue;

			try {
				URL featureURL = new URL(location, "features/" + entry.getId() + "_" + entry.getVersion() + ".jar");
				Feature includedFeature = parseFeature(featureParser, featureURL);
				featuresMap.put(key, includedFeature);
				loadIncludedFeatures(includedFeature, featureParser, featuresMap);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
			try {
				return manager.loadRepository(stateDirURL, null);
			} catch (ProvisionException e) {
				//fall through and create a new repository
			}
			repository = manager.createRepository(stateDirURL, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY);
			repository.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
			return repository;
		} catch (ProvisionException e) {
			LogHelper.log(e);
			throw new IllegalStateException("Couldn't create artifact repository for: " + repositoryName);
		} finally {
			context.ungetService(reference);
		}
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
