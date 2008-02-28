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
package org.eclipse.equinox.internal.p2.updatesite.metadata;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.zip.Checksum;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.generator.features.*;
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.internal.p2.updatesite.Messages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.core.repository.AbstractRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.xml.sax.SAXException;

public class UpdateSiteMetadataRepository extends AbstractRepository implements IMetadataRepository {

	private final IMetadataRepository metadataRepository;
	private static final String FEATURE_VERSION_SEPARATOR = "_"; //$NON-NLS-1$
	private static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	private static final String FEATURE_DIR = "features/"; //$NON-NLS-1$
	private static final String FEATURE_TEMP_FILE = "feature"; //$NON-NLS-1$
	private static final String PROP_SITE_CHECKSUM = "site.checksum"; //$NON-NLS-1$

	public UpdateSiteMetadataRepository(URL location, URL localRepositoryURL, InputStream is, Checksum checksum) throws ProvisionException {
		super("update site: " + location.toExternalForm(), null, null, location, null, null);
		BundleContext context = Activator.getBundleContext();

		metadataRepository = initializeMetadataRepository(context, localRepositoryURL, "update site implementation - " + location.toExternalForm());

		try {
			DefaultSiteParser siteParser = new DefaultSiteParser();
			long start = System.currentTimeMillis();
			SiteModel siteModel = siteParser.parse(is);
			System.out.println("Time Fetching Metadata Site " + location + " was: " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			String savedChecksum = (String) metadataRepository.getProperties().get(PROP_SITE_CHECKSUM);
			String checksumString = Long.toString(checksum.getValue());
			if (savedChecksum != null && savedChecksum.equals(checksumString))
				return;

			metadataRepository.removeAll();

			SiteCategory[] siteCategories = siteModel.getCategories();
			Map categoryNameToFeatureIUs = new HashMap();
			for (int i = 0; i < siteCategories.length; i++) {
				categoryNameToFeatureIUs.put(siteCategories[i].getName(), new HashSet());
			}

			SiteFeature[] siteFeatures = siteModel.getFeatures();
			Map featureKeyToCategoryNames = new HashMap();
			for (int i = 0; i < siteFeatures.length; i++) {
				SiteFeature siteFeature = siteFeatures[i];
				String featureKey = siteFeature.getFeatureIdentifier() + FEATURE_VERSION_SEPARATOR + siteFeature.getFeatureVersion();
				featureKeyToCategoryNames.put(featureKey, siteFeature.getCategoryNames());
			}

			Feature[] features = loadFeaturesFromDigest(location, siteModel);
			if (features == null) {
				features = loadFeaturesFromSiteFeatures(location, siteFeatures);
			}

			Properties extraProperties = new Properties();
			extraProperties.put(IInstallableUnit.PROP_PARTIAL_IU, Boolean.TRUE.toString());
			Set allSiteIUs = new HashSet();
			BundleDescriptionFactory bundleDesciptionFactory = initializeBundleDescriptionFactory(Activator.getBundleContext());

			for (int i = 0; i < features.length; i++) {
				Feature feature = features[i];
				FeatureEntry[] featureEntries = feature.getEntries();
				for (int j = 0; j < featureEntries.length; j++) {
					FeatureEntry entry = featureEntries[j];
					if (entry.isPlugin() && !entry.isRequires()) {
						Dictionary mockManifest = new Properties();
						mockManifest.put("Manifest-Version", "1.0");
						mockManifest.put("Bundle-ManifestVersion", "2");
						mockManifest.put("Bundle-SymbolicName", entry.getId());
						mockManifest.put("Bundle-Version", entry.getVersion());
						BundleDescription bundleDescription = bundleDesciptionFactory.getBundleDescription(mockManifest, null);
						IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(entry.getId(), entry.getVersion());
						IInstallableUnit bundleIU = MetadataGeneratorHelper.createEclipseIU(bundleDescription, null, entry.isUnpack(), key, extraProperties);
						allSiteIUs.add(bundleIU);
					}
				}

				IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureJarIU(feature, true);
				IInstallableUnit groupIU = MetadataGeneratorHelper.createGroupIU(feature, featureIU);

				String featureKey = feature.getId() + FEATURE_VERSION_SEPARATOR + feature.getVersion();
				String[] categoryNames = (String[]) featureKeyToCategoryNames.get(featureKey);
				if (categoryNames != null) {
					for (int j = 0; j < categoryNames.length; j++) {
						Set featureIUList = (Set) categoryNameToFeatureIUs.get(categoryNames[j]);
						if (featureIUList != null) {
							featureIUList.add(groupIU);
						}
					}
				}
				allSiteIUs.add(featureIU);
				allSiteIUs.add(groupIU);
			}

			for (int i = 0; i < siteCategories.length; i++) {
				SiteCategory category = siteCategories[i];
				Set featureIUs = (Set) categoryNameToFeatureIUs.get(category.getName());
				IInstallableUnit categoryIU = MetadataGeneratorHelper.createCategoryIU(category, featureIUs, null);
				allSiteIUs.add(categoryIU);
			}

			IInstallableUnit[] ius = (IInstallableUnit[]) allSiteIUs.toArray(new IInstallableUnit[allSiteIUs.size()]);
			metadataRepository.addInstallableUnits(ius);
			metadataRepository.setProperty(PROP_SITE_CHECKSUM, checksumString);
			System.out.println("Time Fetching Metadata Site and Features for " + location + " was: " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		} catch (SAXException e) {
			String msg = NLS.bind(Messages.UpdateSiteMetadataRepository_ErrorParsingUpdateSite, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
		} catch (IOException e) {
			String msg = NLS.bind(Messages.UpdateSiteMetadataRepository_ErrorReadingUpdateSite, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
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
			String key = siteFeature.getFeatureIdentifier() + FEATURE_VERSION_SEPARATOR + siteFeature.getFeatureVersion();
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

			String key = entry.getId() + FEATURE_VERSION_SEPARATOR + entry.getVersion();
			if (featuresMap.containsKey(key))
				continue;

			try {
				URL featureURL = new URL(location, FEATURE_DIR + entry.getId() + FEATURE_VERSION_SEPARATOR + entry.getVersion() + JAR_EXTENSION);
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

		File featureFile = File.createTempFile(FEATURE_TEMP_FILE, JAR_EXTENSION);
		try {
			FileUtils.copyStream(featureURL.openStream(), false, new BufferedOutputStream(new FileOutputStream(featureFile)), true);
			Feature feature = featureParser.parse(featureFile);
			return feature;
		} finally {
			featureFile.delete();
		}
	}

	private IMetadataRepository initializeMetadataRepository(BundleContext context, URL stateDirURL, String repositoryName) {
		SimpleMetadataRepositoryFactory factory = new SimpleMetadataRepositoryFactory();
		try {
			return factory.load(stateDirURL, null);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		return factory.create(stateDirURL, repositoryName, null);
	}

	private BundleDescriptionFactory initializeBundleDescriptionFactory(BundleContext context) {

		ServiceReference reference = context.getServiceReference(PlatformAdmin.class.getName());
		if (reference == null)
			throw new IllegalStateException(Messages.UpdateSiteMetadataRepository_PlatformAdminNotRegistered);
		PlatformAdmin platformAdmin = (PlatformAdmin) context.getService(reference);
		if (platformAdmin == null)
			throw new IllegalStateException(Messages.UpdateSiteMetadataRepository_PlatformAdminNotRegistered);

		try {
			StateObjectFactory stateObjectFactory = platformAdmin.getFactory();
			return new BundleDescriptionFactory(stateObjectFactory, null);
		} finally {
			context.ungetService(reference);
		}
	}

	public Map getProperties() {
		Map result = new HashMap(metadataRepository.getProperties());
		result.remove(IRepository.PROP_SYSTEM);

		return result;
	}

	public String setProperty(String key, String value) {
		return metadataRepository.setProperty(key, value);
	}

	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		return metadataRepository.query(query, collector, monitor);
	}

	public void removeAll() {
		metadataRepository.removeAll();
	}

	public void addInstallableUnits(IInstallableUnit[] installableUnits) {
		metadataRepository.addInstallableUnits(installableUnits);
	}

	public boolean removeInstallableUnits(Query query, IProgressMonitor monitor) {
		return metadataRepository.removeInstallableUnits(query, monitor);
	}

}
