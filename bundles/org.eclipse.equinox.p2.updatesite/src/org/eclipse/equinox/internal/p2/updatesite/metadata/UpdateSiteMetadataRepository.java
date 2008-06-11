/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ray Braithwood (ray@genuitec.com) - fix for bug 220605
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite.metadata;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.generator.features.*;
import org.eclipse.equinox.internal.p2.updatesite.*;
import org.eclipse.equinox.internal.p2.updatesite.Messages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryEvent;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class UpdateSiteMetadataRepository extends AbstractMetadataRepository {

	public static final String TYPE = "org.eclipse.equinox.p2.updatesite.metadataRepository"; //$NON-NLS-1$
	public static final Integer VERSION = new Integer(1);
	private final IMetadataRepository metadataRepository;
	private static final String FEATURE_VERSION_SEPARATOR = "_"; //$NON-NLS-1$
	private static final String PROP_SITE_CHECKSUM = "site.checksum"; //$NON-NLS-1$

	public UpdateSiteMetadataRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		super(Activator.getRepositoryName(location), TYPE, VERSION.toString(), location, null, null, null);
		// todo progress monitoring
		// loading validates before we create repositories
		UpdateSite updateSite = UpdateSite.load(location, null);
		broadcastAssociateSites(updateSite);

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

		metadataRepository = initializeMetadataRepository(context, localRepositoryURL, "update site implementation - " + location.toExternalForm(), updateSite); //$NON-NLS-1$

		String savedChecksum = (String) metadataRepository.getProperties().get(PROP_SITE_CHECKSUM);
		if (savedChecksum != null && savedChecksum.equals(updateSite.getChecksum()))
			return;
		metadataRepository.removeAll();
		generateMetadata(updateSite);
		metadataRepository.setProperty(PROP_SITE_CHECKSUM, updateSite.getChecksum());
	}

	/**
	 * Broadcast events for any associated sites for this repository so repository
	 * managers are aware of them.
	 */
	private void broadcastAssociateSites(UpdateSite baseSite) {
		if (baseSite == null)
			return;
		URLEntry[] sites = baseSite.getSite().getAssociatedSites();
		if (sites == null || sites.length == 0)
			return;

		IProvisioningEventBus bus = (IProvisioningEventBus) ServiceHelper.getService(Activator.getBundleContext(), IProvisioningEventBus.SERVICE_NAME);
		if (bus == null)
			return;
		for (int i = 0; i < sites.length; i++) {
			try {
				URL siteLocation = new URL(sites[i].getURL());
				bus.publishEvent(new RepositoryEvent(siteLocation, IRepository.TYPE_METADATA, RepositoryEvent.DISCOVERED, true));
				bus.publishEvent(new RepositoryEvent(siteLocation, IRepository.TYPE_ARTIFACT, RepositoryEvent.DISCOVERED, true));
			} catch (MalformedURLException e) {
				LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Site has invalid associate site: " + baseSite.getLocation(), e)); //$NON-NLS-1$
			}
		}

	}

	private void generateMetadata(UpdateSite updateSite) throws ProvisionException {
		SiteModel siteModel = updateSite.getSite();

		// we load the features here to ensure that all site features are fully populated with
		// id and version information before looking at category information
		Feature[] features = updateSite.loadFeatures();

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

			IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureJarIU(feature, true, null);
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
			publishSites(feature);
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

	/*(non-Javadoc)
	 * @see IMetadataRepositoryFactory#validate(URL, IProgressMonitor)
	 */
	public static void validate(URL url, IProgressMonitor monitor) throws ProvisionException {
		UpdateSite.validate(url, monitor);
	}

	private IMetadataRepository initializeMetadataRepository(BundleContext context, URL stateDirURL, String repositoryName, UpdateSite updateSite) {
		SimpleMetadataRepositoryFactory factory = new SimpleMetadataRepositoryFactory();
		try {
			return factory.load(stateDirURL, null);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		Map props = new HashMap(5);
		String mirrors = updateSite.getMirrorsURL();
		if (mirrors != null) {
			props.put(IRepository.PROP_MIRRORS_URL, mirrors);
			//set the mirror base URL relative to the real remote repository rather than our local cache
			props.put(IRepository.PROP_MIRRORS_BASE_URL, getLocation().toExternalForm());
		}
		return factory.create(stateDirURL, repositoryName, null, props);
	}

	private BundleDescriptionFactory initializeBundleDescriptionFactory(BundleContext context) {
		ServiceReference reference = context.getServiceReference(PlatformAdmin.class.getName());
		if (reference == null)
			throw new IllegalStateException(Messages.PlatformAdminNotRegistered);
		PlatformAdmin platformAdmin = (PlatformAdmin) context.getService(reference);
		if (platformAdmin == null)
			throw new IllegalStateException(Messages.PlatformAdminNotRegistered);

		try {
			StateObjectFactory stateObjectFactory = platformAdmin.getFactory();
			return new BundleDescriptionFactory(stateObjectFactory, null);
		} finally {
			context.ungetService(reference);
		}
	}

	public Map getProperties() {
		return metadataRepository.getProperties();
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

	public void initialize(RepositoryState state) {
		//nothing to do
	}

	/**
	 * Broadcast events for any discovery sites associated with the feature
	 * so the repository managers add them to their list of known repositories.
	 */
	private void publishSites(Feature feature) {
		IProvisioningEventBus bus = (IProvisioningEventBus) ServiceHelper.getService(Activator.getBundleContext(), IProvisioningEventBus.SERVICE_NAME);
		if (bus == null)
			return;
		URLEntry[] discoverySites = feature.getDiscoverySites();
		for (int i = 0; i < discoverySites.length; i++)
			publishSite(feature, bus, discoverySites[i].getURL(), false);
		String updateSite = feature.getUpdateSiteURL();
		if (updateSite != null)
			publishSite(feature, bus, updateSite, true);
	}

	/**
	 * Broadcast a discovery event for the given repository location.
	 */
	private void publishSite(Feature feature, IProvisioningEventBus bus, String locationString, boolean isEnabled) {
		try {
			URL siteLocation = new URL(locationString);
			bus.publishEvent(new RepositoryEvent(siteLocation, IRepository.TYPE_METADATA, RepositoryEvent.DISCOVERED, isEnabled));
			bus.publishEvent(new RepositoryEvent(siteLocation, IRepository.TYPE_ARTIFACT, RepositoryEvent.DISCOVERED, isEnabled));
		} catch (MalformedURLException e) {
			LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Feature references invalid site: " + feature.getId(), e)); //$NON-NLS-1$
		}
	}

}
