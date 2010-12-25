/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 *   Sonatype, Inc. - transport split
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.eclipse.URLEntry;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;
import org.eclipse.equinox.spi.p2.publisher.LocalizationHelper;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

/**
 * Action which processes a site.xml and generates categories.  The categorization process
 * relies on IUs for the various features to have already been generated.
 */
public class SiteXMLAction extends AbstractPublisherAction {
	static final private String QUALIFIER = "qualifier"; //$NON-NLS-1$
	private static final VersionSuffixGenerator versionSuffixGenerator = new VersionSuffixGenerator();
	protected UpdateSite updateSite;
	private SiteCategory defaultCategory;
	private HashSet<SiteCategory> defaultCategorySet;
	protected URI location;
	private String categoryQualifier = null;
	private Version categoryVersion = null;

	/**
	 * Creates a SiteXMLAction from a Location (URI) with an optional qualifier to use for category names
	 * @param location The location of the update site
	 * @param categoryQualifier The qualifier to prepend to categories. This qualifier is used
	 * to ensure that the category IDs are unique between update sites. If <b>null</b> a default
	 * qualifier will be generated
	 */
	public SiteXMLAction(URI location, String categoryQualifier) {
		this.location = location;
		this.categoryQualifier = categoryQualifier;
	}

	/**
	 * Creates a SiteXMLAction from an Update site with an optional qualifier to use for category names
	 * @param updateSite The update site 
	 * @param categoryQualifier The qualifier to prepend to categories. This qualifier is used
	 * to ensure that the category IDs are unique between update sites. If <b>null</b> a default
	 * qualifier will be generated
	 */
	public SiteXMLAction(UpdateSite updateSite, String categoryQualifier) {
		this.updateSite = updateSite;
		this.categoryQualifier = categoryQualifier;
	}

	public void setCategoryVersion(String version) {
		categoryVersion = Version.parseVersion(version);
	}

	private void initialize() {
		if (defaultCategory != null)
			return;
		defaultCategory = new SiteCategory();
		defaultCategory.setDescription("Default category for otherwise uncategorized features"); //$NON-NLS-1$
		defaultCategory.setLabel("Uncategorized"); //$NON-NLS-1$
		defaultCategory.setName("Default"); //$NON-NLS-1$
		defaultCategorySet = new HashSet<SiteCategory>(1);
		defaultCategorySet.add(defaultCategory);
	}

	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		if (updateSite == null) {
			try {
				updateSite = UpdateSite.load(location, (Transport) publisherInfo.getMetadataRepository().getProvisioningAgent().getService(Transport.SERVICE_NAME), monitor);
			} catch (ProvisionException e) {
				return new Status(IStatus.ERROR, Activator.ID, Messages.Error_generating_siteXML, e);
			} catch (OperationCanceledException e) {
				return Status.CANCEL_STATUS;
			}
		}
		initialize();
		return generateCategories(publisherInfo, results, monitor);
	}

	private IStatus generateCategories(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		Map<SiteCategory, Set<IInstallableUnit>> categoriesToIUs = new HashMap<SiteCategory, Set<IInstallableUnit>>();
		Map<SiteFeature, Set<SiteCategory>> featuresToCategories = getFeatureToCategoryMappings(publisherInfo);
		for (SiteFeature feature : featuresToCategories.keySet()) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			Collection<IInstallableUnit> ius = getFeatureIU(feature, publisherInfo, results);
			if (ius == null)
				continue;
			Set<SiteCategory> categories = featuresToCategories.get(feature);
			// if there are no categories for this feature then add it to the default category.
			if (categories == null || categories.isEmpty())
				categories = defaultCategorySet;
			for (SiteCategory category : categories) {
				Set<IInstallableUnit> featureIUs = categoriesToIUs.get(category);
				if (featureIUs == null) {
					featureIUs = new HashSet<IInstallableUnit>();
					categoriesToIUs.put(category, featureIUs);
				}
				featureIUs.addAll(ius);
			}
		}
		addSiteIUsToCategories(categoriesToIUs, publisherInfo, results);
		generateCategoryIUs(categoriesToIUs, results);
		return Status.OK_STATUS;
	}

	private void addSiteIUsToCategories(Map<SiteCategory, Set<IInstallableUnit>> categoriesToIUs, IPublisherInfo publisherInfo, IPublisherResult results) {
		if (updateSite == null)
			return;
		SiteModel site = updateSite.getSite();
		if (site == null)
			return;
		SiteIU[] siteIUs = site.getIUs();
		for (SiteIU siteIU : siteIUs) {
			String[] categoryNames = siteIU.getCategoryNames();
			if (categoryNames.length == 0)
				continue;
			Collection<IInstallableUnit> ius = getIUs(siteIU, publisherInfo, results);
			if (ius.size() == 0)
				continue;
			for (String categoryName : categoryNames) {
				SiteCategory category = site.getCategory(categoryName);
				if (category == null)
					continue;
				Set<IInstallableUnit> categoryIUs = categoriesToIUs.get(category);
				if (categoryIUs == null) {
					categoryIUs = new HashSet<IInstallableUnit>();
					categoriesToIUs.put(category, categoryIUs);
				}
				categoryIUs.addAll(ius);
			}
		}
	}

	private Collection<IInstallableUnit> getIUs(SiteIU siteIU, IPublisherInfo publisherInfo, IPublisherResult results) {
		String id = siteIU.getID();
		String range = siteIU.getRange();
		String type = siteIU.getQueryType();
		String expression = siteIU.getQueryExpression();
		Object[] params = siteIU.getQueryParams();
		if (id == null && (type == null || expression == null))
			return CollectionUtils.emptyList();
		IQuery<IInstallableUnit> query = null;
		if (id != null) {
			VersionRange vRange = new VersionRange(range);
			query = QueryUtil.createIUQuery(id, vRange);
		} else if (type.equals("context")) { //$NON-NLS-1$
			query = QueryUtil.createQuery(expression, params);
		} else if (type.equals("match")) //$NON-NLS-1$
			query = QueryUtil.createMatchQuery(expression, params);
		if (query == null)
			return CollectionUtils.emptyList();
		IQueryResult<IInstallableUnit> queryResult = results.query(query, null);
		if (queryResult.isEmpty())
			queryResult = publisherInfo.getMetadataRepository().query(query, null);
		if (queryResult.isEmpty() && publisherInfo.getContextMetadataRepository() != null)
			queryResult = publisherInfo.getContextMetadataRepository().query(query, null);

		return queryResult.toUnmodifiableSet();
	}

	private static final IExpression qualifierMatchExpr = ExpressionUtil.parse("id == $0 && version ~= $1"); //$NON-NLS-1$

	private Collection<IInstallableUnit> getFeatureIU(SiteFeature feature, IPublisherInfo publisherInfo, IPublisherResult results) {
		String id = feature.getFeatureIdentifier() + ".feature.group"; //$NON-NLS-1$
		String versionString = feature.getFeatureVersion();
		Version version = versionString != null && versionString.length() > 0 ? Version.create(versionString) : Version.emptyVersion;
		IQuery<IInstallableUnit> query = null;
		if (version.equals(Version.emptyVersion)) {
			query = QueryUtil.createIUQuery(id);
		} else {
			String qualifier;
			try {
				qualifier = PublisherHelper.toOSGiVersion(version).getQualifier();
			} catch (UnsupportedOperationException e) {
				qualifier = null;
			}
			if (qualifier != null && qualifier.endsWith(QUALIFIER)) {
				VersionRange range = createVersionRange(version.toString());
				IQuery<IInstallableUnit> qualifierQuery = QueryUtil.createMatchQuery(qualifierMatchExpr, id, range);
				query = qualifierQuery;
			} else {
				query = QueryUtil.createLimitQuery(QueryUtil.createIUQuery(id, version), 1);
			}
		}

		IQueryResult<IInstallableUnit> queryResult = results.query(query, null);
		if (queryResult.isEmpty())
			queryResult = publisherInfo.getMetadataRepository().query(query, null);
		if (queryResult.isEmpty() && publisherInfo.getContextMetadataRepository() != null)
			queryResult = publisherInfo.getContextMetadataRepository().query(query, null);

		if (!queryResult.isEmpty())
			return queryResult.toUnmodifiableSet();
		return null;
	}

	protected VersionRange createVersionRange(String versionId) {
		VersionRange range = null;
		if (versionId == null || "0.0.0".equals(versionId)) //$NON-NLS-1$
			range = VersionRange.emptyRange;
		else {
			int qualifierIdx = versionId.indexOf(QUALIFIER);
			if (qualifierIdx != -1) {
				String newVersion = versionId.substring(0, qualifierIdx);
				if (newVersion.endsWith(".")) //$NON-NLS-1$
					newVersion = newVersion.substring(0, newVersion.length() - 1);

				Version lower = Version.parseVersion(newVersion);
				Version upper = null;
				String newQualifier = VersionSuffixGenerator.incrementQualifier(PublisherHelper.toOSGiVersion(lower).getQualifier());
				org.osgi.framework.Version osgiVersion = PublisherHelper.toOSGiVersion(lower);
				if (newQualifier == null)
					upper = Version.createOSGi(osgiVersion.getMajor(), osgiVersion.getMinor(), osgiVersion.getMicro() + 1);
				else
					upper = Version.createOSGi(osgiVersion.getMajor(), osgiVersion.getMinor(), osgiVersion.getMicro(), newQualifier);
				range = new VersionRange(lower, true, upper, false);
			} else {
				range = new VersionRange(Version.parseVersion(versionId), true, Version.parseVersion(versionId), true);
			}
		}
		return range;
	}

	/**
	 * Computes the mapping of features to categories as defined in the site.xml,
	 * if available. Returns an empty map if there is not site.xml, or no categories.
	 * @return A map of SiteFeature -> Set<SiteCategory>.
	 */
	protected Map<SiteFeature, Set<SiteCategory>> getFeatureToCategoryMappings(IPublisherInfo publisherInfo) {
		HashMap<SiteFeature, Set<SiteCategory>> mappings = new HashMap<SiteFeature, Set<SiteCategory>>();
		if (updateSite == null)
			return mappings;
		SiteModel site = updateSite.getSite();
		if (site == null)
			return mappings;

		//copy mirror information from update site to p2 repositories
		String mirrors = site.getMirrorsURI();
		if (mirrors != null) {
			//remove site.xml file reference
			int index = mirrors.indexOf("site.xml"); //$NON-NLS-1$
			if (index != -1)
				mirrors = mirrors.substring(0, index) + mirrors.substring(index + "site.xml".length()); //$NON-NLS-1$
			publisherInfo.getMetadataRepository().setProperty(IRepository.PROP_MIRRORS_URL, mirrors);
			// there does not really need to be an artifact repo but if there is, setup its mirrors.
			if (publisherInfo.getArtifactRepository() != null)
				publisherInfo.getArtifactRepository().setProperty(IRepository.PROP_MIRRORS_URL, mirrors);
		}

		//publish associate sites as repository references
		URLEntry[] associatedSites = site.getAssociatedSites();
		if (associatedSites != null) {
			ArrayList<IRepositoryReference> refs = new ArrayList<IRepositoryReference>(associatedSites.length * 2);
			for (int i = 0; i < associatedSites.length; i++) {
				URLEntry associatedSite = associatedSites[i];
				String siteLocation = associatedSite.getURL();
				try {
					URI associateLocation = new URI(siteLocation);
					String label = associatedSite.getAnnotation();
					refs.add(new RepositoryReference(associateLocation, label, IRepository.TYPE_METADATA, IRepository.ENABLED));
					refs.add(new RepositoryReference(associateLocation, label, IRepository.TYPE_ARTIFACT, IRepository.ENABLED));
				} catch (URISyntaxException e) {
					String message = "Invalid site reference: " + siteLocation; //$NON-NLS-1$
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message));
				}
			}
			publisherInfo.getMetadataRepository().addReferences(refs);
		}

		File siteFile = URIUtil.toFile(updateSite.getLocation());
		if (siteFile != null && siteFile.exists()) {
			File siteParent = siteFile.getParentFile();
			List<String> messageKeys = site.getMessageKeys();
			if (siteParent.isDirectory()) {
				String[] keyStrings = messageKeys.toArray(new String[messageKeys.size()]);
				site.setLocalizations(LocalizationHelper.getDirPropertyLocalizations(siteParent, "site", null, keyStrings)); //$NON-NLS-1$
			} else if (siteFile.getName().endsWith(".jar")) { //$NON-NLS-1$
				String[] keyStrings = messageKeys.toArray(new String[messageKeys.size()]);
				site.setLocalizations(LocalizationHelper.getJarPropertyLocalizations(siteParent, "site", null, keyStrings)); //$NON-NLS-1$
			}
		}

		SiteFeature[] features = site.getFeatures();
		for (int i = 0; i < features.length; i++) {
			//add a mapping for each category this feature belongs to
			String[] categoryNames = features[i].getCategoryNames();
			Set<SiteCategory> categories = new HashSet<SiteCategory>();
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
	protected void generateCategoryIUs(Map<SiteCategory, Set<IInstallableUnit>> categoriesToFeatures, IPublisherResult result) {
		for (SiteCategory category : categoriesToFeatures.keySet()) {
			result.addIU(createCategoryIU(category, categoriesToFeatures.get(category), null), IPublisherResult.NON_ROOT);
		}
	}

	/**
	 * Creates an IU corresponding to an update site category
	 * @param category The category descriptor
	 * @param featureIUs The IUs of the features that belong to the category
	 * @param parentCategory The parent category, or <code>null</code>
	 * @return an IU representing the category
	 */
	public IInstallableUnit createCategoryIU(SiteCategory category, Set<IInstallableUnit> featureIUs, IInstallableUnit parentCategory) {
		InstallableUnitDescription cat = new MetadataFactory.InstallableUnitDescription();
		cat.setSingleton(true);
		String categoryId = buildCategoryId(category.getName());
		cat.setId(categoryId);
		if (categoryVersion == null)
			cat.setVersion(Version.createOSGi(1, 0, 0, versionSuffixGenerator.generateSuffix(featureIUs, CollectionUtils.<IVersionedId> emptyList())));
		else {
			if (categoryVersion.isOSGiCompatible()) {
				org.osgi.framework.Version osgiVersion = PublisherHelper.toOSGiVersion(categoryVersion);
				String qualifier = osgiVersion.getQualifier();
				if (qualifier.endsWith(QUALIFIER)) {
					String suffix = versionSuffixGenerator.generateSuffix(featureIUs, CollectionUtils.<IVersionedId> emptyList());
					qualifier = qualifier.substring(0, qualifier.length() - 9) + suffix;
					categoryVersion = Version.createOSGi(osgiVersion.getMajor(), osgiVersion.getMinor(), osgiVersion.getMicro(), qualifier);
				}
			}
			cat.setVersion(categoryVersion);
		}

		String label = category.getLabel();
		cat.setProperty(IInstallableUnit.PROP_NAME, label != null ? label : category.getName());
		cat.setProperty(IInstallableUnit.PROP_DESCRIPTION, category.getDescription());

		ArrayList<IRequirement> reqsConfigurationUnits = new ArrayList<IRequirement>(featureIUs.size());
		for (IInstallableUnit iu : featureIUs) {
			VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
			reqsConfigurationUnits.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, iu.getFilter(), false, false));
		}
		//note that update sites don't currently support nested categories, but it may be useful to add in the future
		if (parentCategory != null) {
			reqsConfigurationUnits.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, parentCategory.getId(), VersionRange.emptyRange, parentCategory.getFilter(), false, false));
		}
		cat.setRequirements(reqsConfigurationUnits.toArray(new IRequirement[reqsConfigurationUnits.size()]));

		// Create set of provided capabilities
		ArrayList<IProvidedCapability> providedCapabilities = new ArrayList<IProvidedCapability>();
		providedCapabilities.add(PublisherHelper.createSelfCapability(categoryId, cat.getVersion()));

		Map<Locale, Map<String, String>> localizations = category.getLocalizations();
		if (localizations != null) {
			for (Entry<Locale, Map<String, String>> locEntry : localizations.entrySet()) {
				Locale locale = locEntry.getKey();
				Map<String, String> translatedStrings = locEntry.getValue();
				for (Entry<String, String> e : translatedStrings.entrySet()) {
					cat.setProperty(locale.toString() + '.' + e.getKey(), e.getValue());
				}
				providedCapabilities.add(PublisherHelper.makeTranslationCapability(categoryId, locale));
			}
		}

		cat.setCapabilities(providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));

		cat.setArtifacts(new IArtifactKey[0]);
		cat.setProperty(InstallableUnitDescription.PROP_TYPE_CATEGORY, "true"); //$NON-NLS-1$
		return MetadataFactory.createInstallableUnit(cat);
	}

	/**
	 * Creates a qualified category id. This action's qualifier is used if one exists 
	 * or an existing update site's location is used.
	 */
	private String buildCategoryId(String categoryName) {
		if (categoryQualifier != null) {
			if (categoryQualifier.length() > 0)
				return categoryQualifier + "." + categoryName; //$NON-NLS-1$
			return categoryName;
		}
		if (updateSite != null)
			return URIUtil.toUnencodedString(updateSite.getLocation()) + "." + categoryName; //$NON-NLS-1$
		return categoryName;
	}

	protected Transport getTransport(IPublisherInfo info) {
		@SuppressWarnings("rawtypes")
		IRepository repo = info.getMetadataRepository();
		if (repo == null)
			repo = info.getArtifactRepository();
		if (repo == null)
			throw new IllegalStateException("The transport service can not be found."); //$NON-NLS-1$
		return (Transport) repo.getProvisioningAgent().getService(Transport.SERVICE_NAME);
	}
}
