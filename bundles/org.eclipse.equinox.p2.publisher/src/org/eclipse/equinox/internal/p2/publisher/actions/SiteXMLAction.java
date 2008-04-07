/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.actions;

import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.p2.publisher.features.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.osgi.framework.Version;

/**
 * Action which processes a site.xml and generates categories.  The categorization process
 * relies on IUs for the various features to have already been generated.
 */
public class SiteXMLAction extends AbstractPublishingAction {

	private UpdateSite updateSite;
	private SiteCategory defaultCategory;
	private HashSet defaultCategorySet;

	public SiteXMLAction(URL location) {
		try {
			updateSite = UpdateSite.load(location, new NullProgressMonitor());
		} catch (ProvisionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		initialize();
	}

	public SiteXMLAction(UpdateSite updateSite) {
		this.updateSite = updateSite;
		initialize();
	}

	private void initialize() {
		defaultCategory = new SiteCategory();
		defaultCategory.setDescription("Default category for otherwise uncategorized features"); //$NON-NLS-1$
		defaultCategory.setLabel("Uncategorized"); //$NON-NLS-1$
		defaultCategory.setName("Default"); //$NON-NLS-1$
		defaultCategorySet = new HashSet(1);
		defaultCategorySet.add(defaultCategory);
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		generateCategories(info, results);
		return Status.OK_STATUS;
	}

	private void generateCategories(IPublisherInfo info, IPublisherResult results) {
		Map categoriesToFeatureIUs = new HashMap();
		Map featuresToCategories = getFeatureToCategoryMappings(info);
		for (Iterator i = featuresToCategories.keySet().iterator(); i.hasNext();) {
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
	}

	private IInstallableUnit getFeatureIU(SiteFeature feature, IPublisherResult results) {
		String id = MetadataGeneratorHelper.getTransformedId(feature.getFeatureIdentifier(), false, true);
		Version version = new Version(feature.getFeatureIdentifier());
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
				mirrors = mirrors.substring(0, index) + mirrors.substring(index + 9);
			info.getMetadataRepository().setProperty(IRepository.PROP_MIRRORS_URL, mirrors);
			info.getArtifactRepository().setProperty(IRepository.PROP_MIRRORS_URL, mirrors);
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
	 * Generates IUs corresponding to update site categories.
	 * @param categoriesToFeatures Map of SiteCategory ->Set (Feature IUs in that category).
	 * @param result The generator result being built
	 */
	protected void generateCategoryIUs(Map categoriesToFeatures, IPublisherResult result) {
		for (Iterator it = categoriesToFeatures.keySet().iterator(); it.hasNext();) {
			SiteCategory category = (SiteCategory) it.next();
			result.addIU(MetadataGeneratorHelper.createCategoryIU(category, (Set) categoriesToFeatures.get(category), null), IPublisherResult.NON_ROOT);
		}
	}
}
