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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.publisher.BundleDescriptionFactory;
import org.eclipse.equinox.internal.p2.publisher.MetadataGeneratorHelper;
import org.eclipse.equinox.internal.p2.publisher.features.*;
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.internal.p2.updatesite.Messages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.core.repository.AbstractRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.BundleContext;

public class UpdateSiteMetadataRepository extends AbstractRepository implements IMetadataRepository {

	private final IMetadataRepository metadataRepository;
	private static final String FEATURE_VERSION_SEPARATOR = "_"; //$NON-NLS-1$
	private static final String PROP_SITE_CHECKSUM = "site.checksum"; //$NON-NLS-1$

	public UpdateSiteMetadataRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		super("update site: " + location.toExternalForm(), null, null, location, null, null); //$NON-NLS-1$
		// todo progress monitoring
		// loading validates before we create repositories
		UpdateSite updateSite = UpdateSite.load(location, null);

		BundleContext context = Activator.getBundleContext();
		String stateDirName = Integer.toString(location.toExternalForm().hashCode());
		File bundleData = context.getDataFile(null);
		File stateDir = new File(bundleData, stateDirName);
		URL localRepositoryURL;
		try {
			localRepositoryURL = stateDir.toURL();
		} catch (MalformedURLException e) {
			// unexpected
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, Messages.ErrorCreatingRepository, e));
		}

		metadataRepository = initializeMetadataRepository(context, localRepositoryURL, "update site implementation - " + location.toExternalForm()); //$NON-NLS-1$

		String savedChecksum = (String) metadataRepository.getProperties().get(PROP_SITE_CHECKSUM);
		if (savedChecksum != null && savedChecksum.equals(updateSite.getChecksum()))
			return;
		metadataRepository.setProperty(PROP_SITE_CHECKSUM, updateSite.getChecksum());
		metadataRepository.removeAll();
		generateMetadata(updateSite);
	}

	private void generateMetadata(UpdateSite updateSite) throws ProvisionException {
		SiteModel siteModel = updateSite.getSite();
		SiteCategory[] siteCategories = siteModel.getCategories();
		Map categoryNameToFeatureIUs = new HashMap();
		for (int i = 0; i < siteCategories.length; i++)
			categoryNameToFeatureIUs.put(siteCategories[i].getName(), new HashSet());

		SiteFeature[] siteFeatures = siteModel.getFeatures();
		Map featureKeyToCategoryNames = new HashMap();
		for (int i = 0; i < siteFeatures.length; i++) {
			SiteFeature siteFeature = siteFeatures[i];
			String featureKey = siteFeature.getFeatureIdentifier() + FEATURE_VERSION_SEPARATOR + siteFeature.getFeatureVersion();
			featureKeyToCategoryNames.put(featureKey, siteFeature.getCategoryNames());
		}

		Feature[] features = updateSite.loadFeatures();
		Properties extraProperties = new Properties();
		extraProperties.put(IInstallableUnit.PROP_PARTIAL_IU, Boolean.TRUE.toString());
		Set allSiteIUs = new HashSet();
		BundleDescriptionFactory bundleDesciptionFactory = BundleDescriptionFactory.getBundleDescriptionFactory(Activator.getBundleContext());

		for (int i = 0; i < features.length; i++) {
			Feature feature = features[i];
			FeatureEntry[] featureEntries = feature.getEntries();
			for (int j = 0; j < featureEntries.length; j++) {
				FeatureEntry entry = featureEntries[j];
				if (entry.isPlugin() && !entry.isRequires()) {
					Dictionary mockManifest = new Properties();
					mockManifest.put("Manifest-Version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
					mockManifest.put("Bundle-ManifestVersion", "2"); //$NON-NLS-1$ //$NON-NLS-2$
					mockManifest.put("Bundle-SymbolicName", entry.getId()); //$NON-NLS-1$
					mockManifest.put("Bundle-Version", entry.getVersion()); //$NON-NLS-1$
					BundleDescription bundleDescription = bundleDesciptionFactory.getBundleDescription(mockManifest, null);
					IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(entry.getId(), entry.getVersion());
					IInstallableUnit[] bundleIUs = MetadataGeneratorHelper.createEclipseIU(bundleDescription, null, entry.isUnpack(), key, extraProperties);
					for (int n = 0; n < bundleIUs.length; n++) {
						allSiteIUs.add(bundleIUs[n]);
					}
				}
			}

			IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureJarIU(feature, null, true, null);
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
