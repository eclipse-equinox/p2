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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.generator.features.*;
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.internal.p2.updatesite.Messages;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.internal.provisional.spi.p2.core.repository.AbstractRepository;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.xml.sax.SAXException;

public class UpdateSiteArtifactRepository extends AbstractRepository implements IArtifactRepository {

	private static final String PROP_FORCE_THREADING = "eclipse.p2.force.threading"; //$NON-NLS-1$
	private static final String SITE_FILE = "site.xml"; //$NON-NLS-1$
	private static final String DIR_SEPARATOR = "/"; //$NON-NLS-1$

	private final IArtifactRepository artifactRepository;

	public UpdateSiteArtifactRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
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
		InputStream is = getSiteInputStream(location);
		try {

			DefaultSiteParser siteParser = new DefaultSiteParser();
			Checksum checksum = new CRC32();
			is = new CheckedInputStream(new BufferedInputStream(is), checksum);
			SiteModel siteModel = siteParser.parse(is);

			String savedChecksum = (String) artifactRepository.getProperties().get("site.checksum");
			String checksumString = Long.toString(checksum.getValue());
			if (savedChecksum != null && savedChecksum.equals(checksumString))
				return;

			if (!location.getProtocol().equals("file"))
				artifactRepository.setProperty(PROP_FORCE_THREADING, "true"); //$NON-NLS-1$
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
				URL featureURL = getFileURL(location, "features/" + feature.getId() + "_" + feature.getVersion() + ".jar");
				featureArtifactDescriptor.setRepositoryProperty("artifact.reference", featureURL.toExternalForm());
				allSiteArtifacts.add(featureArtifactDescriptor);

				FeatureEntry[] featureEntries = feature.getEntries();
				for (int j = 0; j < featureEntries.length; j++) {
					FeatureEntry entry = featureEntries[j];
					if (entry.isPlugin() && !entry.isRequires()) {
						IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(entry.getId(), entry.getVersion());
						ArtifactDescriptor artifactDescriptor = new ArtifactDescriptor(key);
						URL pluginURL = getFileURL(location, "plugins/" + entry.getId() + "_" + entry.getVersion() + ".jar");
						artifactDescriptor.setRepositoryProperty("artifact.reference", pluginURL.toExternalForm());
						allSiteArtifacts.add(artifactDescriptor);
					}
				}
			}

			IArtifactDescriptor[] descriptors = (IArtifactDescriptor[]) allSiteArtifacts.toArray(new IArtifactDescriptor[allSiteArtifacts.size()]);
			artifactRepository.addDescriptors(descriptors);
		} catch (SAXException e) {
			String msg = NLS.bind(Messages.UpdateSiteArtifactRepository_ErrorParsingUpdateSite, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} catch (IOException e) {
			String msg = NLS.bind(Messages.UpdateSiteArtifactRepository_ErrorReadingUpdateSite, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} finally {
			safeClose(is);
		}
	}

	public static void validate(URL url, IProgressMonitor monitor) throws ProvisionException {
		InputStream is = null;
		try {
			is = getSiteInputStream(url);
		} finally {
			safeClose(is);
		}
	}

	private static InputStream getSiteInputStream(URL url) throws ProvisionException {
		try {
			URL siteURL = getSiteURL(url);
			return siteURL.openStream();
		} catch (MalformedURLException e) {
			String msg = NLS.bind(Messages.UpdateSiteArtifactRepository_InvalidRepositoryLocation, url);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_INVALID_LOCATION, msg, e));
		} catch (IllegalArgumentException e) {
			//see bug 221600 - URL.openStream can throw IllegalArgumentException
			String msg = NLS.bind(Messages.UpdateSiteMetadataRepository_InvalidRepositoryLocation, url);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_INVALID_LOCATION, msg, e));
		} catch (IOException e) {
			String msg = NLS.bind(Messages.UpdateSiteArtifactRepository_ErrorReadingSite, url);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, msg, e));
		}
	}

	private static void safeClose(InputStream stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (IOException e) {
			//ignore
		}
	}

	public static URL getSiteURL(URL url) throws MalformedURLException {
		if (url.getPath().endsWith(SITE_FILE))
			return url;

		if (url.getPath().endsWith(DIR_SEPARATOR))
			return new URL(url.toExternalForm() + SITE_FILE);

		return new URL(url.toExternalForm() + DIR_SEPARATOR + SITE_FILE);
	}

	public static URL getFileURL(URL url, String fileName) throws MalformedURLException {
		if (url.getPath().endsWith(fileName))
			return url;

		if (url.getPath().endsWith(SITE_FILE))
			return new URL(url, fileName);

		if (url.getPath().endsWith(DIR_SEPARATOR))
			return new URL(url.toExternalForm() + fileName);

		return new URL(url.toExternalForm() + DIR_SEPARATOR + fileName);
	}

	private Feature[] loadFeaturesFromDigest(URL url, SiteModel siteModel) {
		try {
			URL digestURL = getFileURL(url, "digest.zip");
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
				URL featureURL = getFileURL(location, siteFeature.getURLString());
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
				URL featureURL = getFileURL(location, "features/" + entry.getId() + "_" + entry.getVersion() + ".jar");
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
		SimpleArtifactRepositoryFactory factory = new SimpleArtifactRepositoryFactory();
		try {
			return factory.load(stateDirURL, null);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		return factory.create(stateDirURL, repositoryName, null);
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

	public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
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
