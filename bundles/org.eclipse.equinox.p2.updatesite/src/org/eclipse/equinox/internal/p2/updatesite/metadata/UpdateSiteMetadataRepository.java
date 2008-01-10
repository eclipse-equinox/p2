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
import java.util.zip.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.generator.features.*;
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.generator.*;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.Query;
import org.eclipse.equinox.spi.p2.core.repository.AbstractRepository;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.xml.sax.SAXException;

public class UpdateSiteMetadataRepository extends AbstractRepository implements IMetadataRepository {

	private final IMetadataRepository metadataRepository;

	public UpdateSiteMetadataRepository(URL location) {
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
		metadataRepository = initializeMetadataRepository(context, localRepositoryURL, "update site implementation - " + location.toExternalForm());

		try {

			DefaultSiteParser siteParser = new DefaultSiteParser();
			long start = System.currentTimeMillis();
			Checksum checksum = new CRC32();
			InputStream is = new CheckedInputStream(new BufferedInputStream(location.openStream()), checksum);
			SiteModel siteModel = siteParser.parse(is);
			System.out.println("Time Fetching Metadata Site " + location + " was: " + (System.currentTimeMillis() - start) + " ms");

			String savedChecksum = (String) metadataRepository.getProperties().get("site.checksum");
			String checksumString = Long.toString(checksum.getValue());
			if (savedChecksum != null && savedChecksum.equals(checksumString))
				return;

			metadataRepository.setProperty("site.checksum", checksumString);
			metadataRepository.removeAll();

			Map categoryNameToFeatureIUs = new HashMap();
			SiteCategory[] siteCategories = siteModel.getCategories();
			for (int i = 0; i < siteCategories.length; i++) {
				categoryNameToFeatureIUs.put(siteCategories[i].getName(), new HashSet());
			}

			Set allSiteIUs = new HashSet();
			SiteFeature[] siteFeatures = siteModel.getFeatures();

			FeatureParser featureParser = new FeatureParser();
			BundleDescriptionFactory bundleDesciptionFactory = initializeBundleDescriptionFactory(Activator.getBundleContext());
			System.out.println("Retrieving " + siteFeatures.length + " features");
			for (int i = 0; i < siteFeatures.length; i++) {
				SiteFeature siteFeature = siteFeatures[i];
				System.out.println(siteFeature.getFeatureIdentifier());
				URL featureURL = new URL(location, siteFeature.getURLString());
				Feature feature = parseFeature(featureParser, featureURL);
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
						IInstallableUnit bundleIU = MetadataGeneratorHelper.createBundleIU(bundleDescription, null, entry.isUnpack(), key);
						allSiteIUs.add(bundleIU);
					}
				}

				IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureIU(feature, false);
				IInstallableUnit groupIU = MetadataGeneratorHelper.createGroupIU(feature, featureIU);
				String[] categoryNames = siteFeature.getCategoryNames();
				for (int j = 0; j < categoryNames.length; j++) {
					Set featureIUList = (Set) categoryNameToFeatureIUs.get(categoryNames[j]);
					if (featureIUList != null) {
						featureIUList.add(groupIU);
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
			System.out.println("Time Fetching Metadata Site and Features for " + location + " was: " + (System.currentTimeMillis() - start) + " ms");

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

	private IMetadataRepository initializeMetadataRepository(BundleContext context, URL stateDirURL, String repositoryName) {
		ServiceReference reference = context.getServiceReference(IMetadataRepositoryManager.class.getName());
		if (reference == null)
			throw new IllegalStateException("MetadataRepositoryManager not registered.");

		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException("MetadataRepositoryManager not registered.");

		IMetadataRepository repository = null;
		try {
			repository = manager.loadRepository(stateDirURL, null);
			if (repository == null) {
				repository = manager.createRepository(stateDirURL, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
				repository.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
			}
		} finally {
			context.ungetService(reference);
		}

		if (repository == null)
			throw new IllegalStateException("Couldn't create metadata repository for: " + repositoryName);

		return repository;
	}

	private BundleDescriptionFactory initializeBundleDescriptionFactory(BundleContext context) {

		ServiceReference reference = context.getServiceReference(PlatformAdmin.class.getName());
		if (reference == null)
			throw new IllegalStateException("PlatformAdmin not registered.");
		PlatformAdmin platformAdmin = (PlatformAdmin) context.getService(reference);
		if (platformAdmin == null)
			throw new IllegalStateException("PlatformAdmin not registered.");

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
