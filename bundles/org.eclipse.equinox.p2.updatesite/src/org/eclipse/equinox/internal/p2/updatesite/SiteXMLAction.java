/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.publisher.eclipse.URLEntry;
import org.eclipse.equinox.spi.p2.publisher.LocalizationHelper;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * Action which processes a site.xml and generates categories.  The categorization process
 * relies on IUs for the various features to have already been generated.
 */
public class SiteXMLAction extends AbstractPublisherAction {

	private static final String PROTOCOL_FILE = "file"; //$NON-NLS-1$
	private UpdateSite updateSite;
	private SiteCategory defaultCategory;
	private HashSet defaultCategorySet;
	private URL location;

	public SiteXMLAction(URL location) {
		this.location = location;
	}

	public SiteXMLAction(UpdateSite updateSite) {
		this.updateSite = updateSite;
	}

	private void initialize() {
		if (defaultCategory != null)
			return;
		defaultCategory = new SiteCategory();
		defaultCategory.setDescription("Default category for otherwise uncategorized features"); //$NON-NLS-1$
		defaultCategory.setLabel("Uncategorized"); //$NON-NLS-1$
		defaultCategory.setName("Default"); //$NON-NLS-1$
		defaultCategorySet = new HashSet(1);
		defaultCategorySet.add(defaultCategory);
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
		if (updateSite == null) {
			try {
				updateSite = UpdateSite.load(location, monitor);
			} catch (ProvisionException e) {
				return new Status(IStatus.ERROR, Activator.ID, "Error generating site xml action.", e);
			} catch (OperationCanceledException e) {
				return Status.CANCEL_STATUS;
			}
		}
		initialize();
		return generateCategories(info, results, monitor);
	}

	private IStatus generateCategories(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
		Map categoriesToFeatureIUs = new HashMap();
		Map featuresToCategories = getFeatureToCategoryMappings(info);
		for (Iterator i = featuresToCategories.keySet().iterator(); i.hasNext();) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			SiteFeature feature = (SiteFeature) i.next();
			IInstallableUnit iu = getFeatureIU(feature, results);
			Set categories = (Set) featuresToCategories.get(feature);
			// if there are no categories for this feature then add it to the default category.
			if (categories == null || categories.isEmpty())
				categories = defaultCategorySet;
			for (Iterator it = categories.iterator(); it.hasNext();) {
				SiteCategory category = (SiteCategory) it.next();
				Set featureIUs = (Set) categoriesToFeatureIUs.get(category);
				if (featureIUs == null) {
					featureIUs = new HashSet();
					categoriesToFeatureIUs.put(category, featureIUs);
				}
				featureIUs.add(iu);
			}
		}
		generateCategoryIUs(categoriesToFeatureIUs, results);
		return Status.OK_STATUS;
	}

	private IInstallableUnit getFeatureIU(SiteFeature feature, IPublisherResult results) {
		String id = FeaturesAction.getTransformedId(feature.getFeatureIdentifier(), false, true);
		Version version = new Version(feature.getFeatureVersion());
		// TODO look elsewhere as well.  Perhaps in the metadata repos and some advice.
		Collection ius = results.getIUs(id, null);
		for (Iterator i = ius.iterator(); i.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) i.next();
			if (iu.getVersion().equals(version))
				return iu;
		}
		return null;
	}

	/**
	 * Computes the mapping of features to categories as defined in the site.xml,
	 * if available. Returns an empty map if there is not site.xml, or no categories.
	 * @return A map of SiteFeature -> Set<SiteCategory>.
	 */
	protected Map getFeatureToCategoryMappings(IPublisherInfo info) {
		HashMap mappings = new HashMap();
		if (updateSite == null)
			return mappings;
		SiteModel site = updateSite.getSite();
		if (site == null)
			return mappings;

		//copy mirror information from update site to p2 repositories
		String mirrors = site.getMirrorsURL();
		if (mirrors != null) {
			//remove site.xml file reference
			int index = mirrors.indexOf("site.xml"); //$NON-NLS-1$
			if (index != -1)
				mirrors = mirrors.substring(0, index) + mirrors.substring(index + "site.xml".length()); //$NON-NLS-1$
			info.getMetadataRepository().setProperty(IRepository.PROP_MIRRORS_URL, mirrors);
			// there does not really need to be an artifact repo but if there is, setup its mirrors.
			if (info.getArtifactRepository() != null)
				info.getArtifactRepository().setProperty(IRepository.PROP_MIRRORS_URL, mirrors);
		}

		//publish associate sites as repository references
		URLEntry[] associatedSites = site.getAssociatedSites();
		if (associatedSites != null)
			for (int i = 0; i < associatedSites.length; i++)
				generateSiteReference(associatedSites[i].getURL(), null, info.getMetadataRepository());

		if (PROTOCOL_FILE.equals(updateSite.getLocation().getProtocol())) {
			File siteFile = new File(updateSite.getLocation().getFile());
			if (siteFile.exists()) {
				File siteParent = siteFile.getParentFile();

				List messageKeys = site.getMessageKeys();
				if (siteParent.isDirectory()) {
					String[] keyStrings = (String[]) messageKeys.toArray(new String[messageKeys.size()]);
					site.setLocalizations(LocalizationHelper.getDirPropertyLocalizations(siteParent, "site", null, keyStrings)); //$NON-NLS-1$
				} else if (siteFile.getName().endsWith(".jar")) { //$NON-NLS-1$
					String[] keyStrings = (String[]) messageKeys.toArray(new String[messageKeys.size()]);
					site.setLocalizations(LocalizationHelper.getJarPropertyLocalizations(siteParent, "site", null, keyStrings)); //$NON-NLS-1$
				}
			}
		}

		SiteFeature[] features = site.getFeatures();
		for (int i = 0; i < features.length; i++) {
			//add a mapping for each category this feature belongs to
			String[] categoryNames = features[i].getCategoryNames();
			Set categories = new HashSet();
			mappings.put(features[i], categories);
			for (int j = 0; j < categoryNames.length; j++) {
				SiteCategory category = site.getCategory(categoryNames[j]);
				if (category != null)
					categories.add(category);
			}
		}
		return mappings;
	}

	/**
	 * Generates and publishes a reference to an update site location
	 * @param location The update site location
	 * @param featureId the identifier of the feature where the error occurred, or null
	 * @param metadataRepo The repo into which the references are added
	 */
	private void generateSiteReference(String location, String featureId, IMetadataRepository metadataRepo) {
		try {
			URL associateLocation = new URL(location);
			metadataRepo.addReference(associateLocation, IRepository.TYPE_METADATA, IRepository.ENABLED);
			metadataRepo.addReference(associateLocation, IRepository.TYPE_ARTIFACT, IRepository.ENABLED);
		} catch (MalformedURLException e) {
			String message = "Invalid site reference: " + location; //$NON-NLS-1$
			if (featureId != null)
				message = message + " in feature: " + featureId; //$NON-NLS-1$
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message));
		}
	}

	/**
	 * Generates IUs corresponding to update site categories.
	 * @param categoriesToFeatures Map of SiteCategory ->Set (Feature IUs in that category).
	 * @param result The generator result being built
	 */
	protected void generateCategoryIUs(Map categoriesToFeatures, IPublisherResult result) {
		for (Iterator it = categoriesToFeatures.keySet().iterator(); it.hasNext();) {
			SiteCategory category = (SiteCategory) it.next();
			result.addIU(createCategoryIU(category, (Set) categoriesToFeatures.get(category), null), IPublisherResult.NON_ROOT);
		}
	}

	/**
	 * Creates an IU corresponding to an update site category
	 * @param category The category descriptor
	 * @param featureIUs The IUs of the features that belong to the category
	 * @param parentCategory The parent category, or <code>null</code>
	 * @return an IU representing the category
	 */
	public static IInstallableUnit createCategoryIU(SiteCategory category, Set featureIUs, IInstallableUnit parentCategory) {
		InstallableUnitDescription cat = new MetadataFactory.InstallableUnitDescription();
		cat.setSingleton(true);
		String categoryId = category.getName();
		cat.setId(categoryId);
		cat.setVersion(Version.emptyVersion);
		cat.setProperty(IInstallableUnit.PROP_NAME, category.getLabel());
		cat.setProperty(IInstallableUnit.PROP_DESCRIPTION, category.getDescription());

		ArrayList reqsConfigurationUnits = new ArrayList(featureIUs.size());
		for (Iterator iterator = featureIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
			reqsConfigurationUnits.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, iu.getFilter(), false, false));
		}
		//note that update sites don't currently support nested categories, but it may be useful to add in the future
		if (parentCategory != null) {
			reqsConfigurationUnits.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, parentCategory.getId(), VersionRange.emptyRange, parentCategory.getFilter(), false, false));
		}
		cat.setRequiredCapabilities((RequiredCapability[]) reqsConfigurationUnits.toArray(new RequiredCapability[reqsConfigurationUnits.size()]));

		// Create set of provided capabilities
		ArrayList providedCapabilities = new ArrayList();
		providedCapabilities.add(PublisherHelper.createSelfCapability(categoryId, Version.emptyVersion));

		Map localizations = category.getLocalizations();
		if (localizations != null) {
			for (Iterator iter = localizations.keySet().iterator(); iter.hasNext();) {
				Locale locale = (Locale) iter.next();
				Properties translatedStrings = (Properties) localizations.get(locale);
				Enumeration propertyKeys = translatedStrings.propertyNames();
				while (propertyKeys.hasMoreElements()) {
					String nextKey = (String) propertyKeys.nextElement();
					cat.setProperty(locale.toString() + '.' + nextKey, translatedStrings.getProperty(nextKey));
				}
				providedCapabilities.add(PublisherHelper.makeTranslationCapability(categoryId, locale));
			}
		}

		cat.setCapabilities((ProvidedCapability[]) providedCapabilities.toArray(new ProvidedCapability[providedCapabilities.size()]));

		cat.setArtifacts(new IArtifactKey[0]);
		cat.setProperty(IInstallableUnit.PROP_TYPE_CATEGORY, "true"); //$NON-NLS-1$
		return MetadataFactory.createInstallableUnit(cat);
	}

}
