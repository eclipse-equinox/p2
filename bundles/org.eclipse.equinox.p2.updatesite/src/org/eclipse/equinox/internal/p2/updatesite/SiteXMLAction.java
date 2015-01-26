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
 *   Red Hat Inc. - 383795 (bundle element)
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;
import org.eclipse.equinox.spi.p2.publisher.LocalizationHelper;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

/**
 * Action which processes a site.xml and generates categories.  The categorization process
 * relies on IUs for the various features to have already been generated.
 */
public class SiteXMLAction extends AbstractPublisherAction {
	static final private String QUALIFIER = "qualifier"; //$NON-NLS-1$
	static final private String P_STATS_URI = "p2.statsURI"; //$NON-NLS-1$
	static final private String P_STATS_MARKER = "download.stats"; //$NON-NLS-1$
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
		initializeRepoFromSite(publisherInfo);
		IStatus markingStats = markStatsArtifacts(publisherInfo, results, monitor);
		if (markingStats.isOK()) {
			return generateCategories(publisherInfo, results, monitor);
		}
		return markingStats;
	}

	private IStatus markStatsArtifacts(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		IArtifactRepository artifactRepo = publisherInfo.getArtifactRepository();
		SiteModel site = updateSite.getSite();
		// process all features listed and mark artifacts
		SiteFeature[] features = site.getStatsFeatures();
		if (features != null && artifactRepo != null) {
			for (SiteFeature feature : features) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				Collection<IInstallableUnit> ius = getFeatureIU(feature, publisherInfo, results);
				if (ius != null) {
					for (IInstallableUnit iu : ius) {
						IArtifactKey key = FeaturesAction.createFeatureArtifactKey(feature.getFeatureIdentifier(), iu.getVersion().toString());
						IArtifactDescriptor[] descriptors = artifactRepo.getArtifactDescriptors(key);
						if (descriptors.length > 0 && descriptors[0] instanceof ArtifactDescriptor) {
							HashMap<String, String> map = new HashMap<String, String>();
							map.put(P_STATS_MARKER, feature.getFeatureIdentifier());
							((ArtifactDescriptor) descriptors[0]).addProperties(map);
						}
					}
				}
			}
		}
		// process all bundles listed and mark artifacts
		SiteBundle[] bundles = site.getStatsBundles();
		if (bundles != null && artifactRepo != null) {
			for (SiteBundle bundle : bundles) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				Collection<IInstallableUnit> ius = getBundleIU(bundle, publisherInfo, results);
				if (ius != null) {
					for (IInstallableUnit iu : ius) {
						IArtifactKey key = BundlesAction.createBundleArtifactKey(iu.getId(), iu.getVersion().toString());
						IArtifactDescriptor[] descriptors = artifactRepo.getArtifactDescriptors(key);
						if (descriptors.length > 0 && descriptors[0] instanceof ArtifactDescriptor) {
							HashMap<String, String> map = new HashMap<String, String>();
							map.put(P_STATS_MARKER, iu.getId());
							((ArtifactDescriptor) descriptors[0]).addProperties(map);
						}
					}
				}
			}
		}
		// If there was no artifact repository available and stats were to be tracked, issue
		// a warning.
		boolean markingBundles = bundles != null && bundles.length > 0;
		boolean markingFeatures = features != null && features.length > 0;
		if (artifactRepo == null && (markingBundles || markingFeatures))
			return new Status(IStatus.WARNING, Activator.ID, "Artifact repository was not specified so stats properties could not be published."); //$NON-NLS-1$
		return Status.OK_STATUS;

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
				Set<IInstallableUnit> iusInCategory = categoriesToIUs.get(category);
				if (iusInCategory == null) {
					iusInCategory = new HashSet<IInstallableUnit>();
					categoriesToIUs.put(category, iusInCategory);
				}
				iusInCategory.addAll(ius);
			}
		}
		// Bundles -- bug 378338
		Map<SiteBundle, Set<SiteCategory>> bundlesToCategories = getBundleToCategoryMappings(publisherInfo);
		for (SiteBundle bundle : bundlesToCategories.keySet()) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			Collection<IInstallableUnit> ius = getBundleIU(bundle, publisherInfo, results);
			if (ius == null)
				continue;
			Set<SiteCategory> categories = bundlesToCategories.get(bundle);
			// if there are no categories for this feature then add it to the default category.
			if (categories == null || categories.isEmpty())
				categories = defaultCategorySet;
			for (SiteCategory category : categories) {
				Set<IInstallableUnit> iusInCategory = categoriesToIUs.get(category);
				if (iusInCategory == null) {
					iusInCategory = new HashSet<IInstallableUnit>();
					categoriesToIUs.put(category, iusInCategory);
				}
				iusInCategory.addAll(ius);
			}
		}

		addSiteIUsToCategories(categoriesToIUs, publisherInfo, results);
		generateCategoryIUs(categoriesToIUs, results);
		return Status.OK_STATUS;
	}

	private void addSiteIUsToCategories(Map<SiteCategory, Set<IInstallableUnit>> categoriesToIUs, IPublisherInfo publisherInfo, IPublisherResult results) {
		Map<SiteIU, Set<SiteCategory>> iusToCategories = getIUToCategoryMappings(publisherInfo);
		SiteModel site = updateSite.getSite();
		if (site == null)
			return;
		SiteIU[] siteIUs = site.getIUs();
		for (SiteIU siteIU : siteIUs) {
			Collection<IInstallableUnit> ius = getIUs(siteIU, publisherInfo, results);
			if (ius == null)
				continue;
			Set<SiteCategory> categories = iusToCategories.get(siteIU);
			// if there are no categories for this feature then add it to the default category.
			if (categories == null || categories.isEmpty())
				categories = defaultCategorySet;
			for (SiteCategory category : categories) {
				Set<IInstallableUnit> iusInCategory = categoriesToIUs.get(category);
				if (iusInCategory == null) {
					iusInCategory = new HashSet<IInstallableUnit>();
					categoriesToIUs.put(category, iusInCategory);
				}
				iusInCategory.addAll(ius);
			}
		}
	}

	private Map<SiteIU, Set<SiteCategory>> getIUToCategoryMappings(IPublisherInfo publisherInfo) {
		HashMap<SiteIU, Set<SiteCategory>> mappings = new HashMap<SiteIU, Set<SiteCategory>>();
		if (updateSite == null)
			return mappings;
		SiteModel site = updateSite.getSite();
		if (site == null)
			return mappings;

		SiteIU[] ius = site.getIUs();
		for (int i = 0; i < ius.length; i++) {
			//add a mapping for each category this feature belongs to
			String[] categoryNames = ius[i].getCategoryNames();
			Set<SiteCategory> categories = new HashSet<SiteCategory>();
			mappings.put(ius[i], categories);
			for (int j = 0; j < categoryNames.length; j++) {
				SiteCategory category = site.getCategory(categoryNames[j]);
				if (category != null)
					categories.add(category);
			}
		}
		return mappings;

	}

	private Collection<IInstallableUnit> getIUs(SiteIU siteIU, IPublisherInfo publisherInfo, IPublisherResult results) {
		String id = siteIU.getID();
		String range = siteIU.getRange();
		String type = siteIU.getQueryType();
		String expression = siteIU.getQueryExpression();
		Object[] params = siteIU.getQueryParams();
		if (id == null && (type == null || expression == null))
			return Collections.<IInstallableUnit> emptyList();
		IQuery<IInstallableUnit> query = null;
		if (id != null) {
			VersionRange vRange = new VersionRange(range);
			query = QueryUtil.createIUQuery(id, vRange);
		} else if (type.equals("context")) { //$NON-NLS-1$
			query = QueryUtil.createQuery(expression, params);
		} else if (type.equals("match")) //$NON-NLS-1$
			query = QueryUtil.createMatchQuery(expression, params);
		if (query == null)
			return Collections.<IInstallableUnit> emptyList();
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

	private Collection<IInstallableUnit> getBundleIU(SiteBundle bundle, IPublisherInfo publisherInfo, IPublisherResult results) {
		String id = bundle.getBundleIdentifier();
		String versionString = bundle.getBundleVersion();
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

		SiteFeature[] features = site.getFeatures();
		for (int i = 0; i < features.length; i++) {
			//add a mapping for each category this feature belongs to
			String[] categoryNames = features[i].getCategoryNames();
			Set<SiteCategory> categories = mappings.get(features[i]);
			if (categories == null) {
				categories = new HashSet<SiteCategory>();
				mappings.put(features[i], categories);
			}
			for (int j = 0; j < categoryNames.length; j++) {
				SiteCategory category = site.getCategory(categoryNames[j]);
				if (category != null)
					categories.add(category);
			}
		}
		return mappings;
	}

	/**
	 * Computes the mapping of bundles to categories as defined in the site.xml,
	 * if available. Returns an empty map if there is not site.xml, or no categories.
	 * @return A map of SiteBundle -> Set<SiteCategory>.
	 */
	protected Map<SiteBundle, Set<SiteCategory>> getBundleToCategoryMappings(IPublisherInfo publisherInfo) {
		HashMap<SiteBundle, Set<SiteCategory>> mappings = new HashMap<SiteBundle, Set<SiteCategory>>();
		if (updateSite == null)
			return mappings;
		SiteModel site = updateSite.getSite();
		if (site == null)
			return mappings;

		SiteBundle[] bundles = site.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			//add a mapping for each category this feature belongs to
			String[] categoryNames = bundles[i].getCategoryNames();
			Set<SiteCategory> categories = new HashSet<SiteCategory>();
			mappings.put(bundles[i], categories);
			for (int j = 0; j < categoryNames.length; j++) {
				SiteCategory category = site.getCategory(categoryNames[j]);
				if (category != null)
					categories.add(category);
			}
		}
		return mappings;
	}

	/**
	 * Initializes new p2 repository attributes such as mirror info, associate sites, localization...
	 * @param publisherInfo configuration for output repository
	 */
	private void initializeRepoFromSite(IPublisherInfo publisherInfo) {
		SiteModel site = updateSite.getSite();
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

		//publish repository references from category file
		IRepositoryReference[] refs = site.getRepositoryReferences();
		if (refs != null) {
			ArrayList<IRepositoryReference> toAdd = new ArrayList<IRepositoryReference>(Arrays.asList(refs));
			publisherInfo.getMetadataRepository().addReferences(toAdd);
		}

		// publish download stats URL from category file
		String statsURI = site.getStatsURI();
		if (statsURI != null && statsURI.length() > 0) {
			if (publisherInfo.getArtifactRepository() != null)
				publisherInfo.getArtifactRepository().setProperty(P_STATS_URI, statsURI);
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
	}

	/**
	 * Generates IUs corresponding to update site categories.
	 * @param categoriesToIUs Map of SiteCategory ->Set (Feature IUs in that category).
	 * @param result The generator result being built
	 */
	protected void generateCategoryIUs(Map<SiteCategory, Set<IInstallableUnit>> categoriesToIUs, IPublisherResult result) {
		Map<String, SiteCategory> nameToCategory = new HashMap<String, SiteCategory>();
		for (SiteCategory category : this.updateSite.getSite().getCategories()) {
			nameToCategory.put(category.getName(), category);
		}
		final Map<SiteCategory, Set<SiteCategory>> categoryToNestedCategories = new HashMap<SiteCategory, Set<SiteCategory>>();
		for (SiteCategory category : this.updateSite.getSite().getCategories()) {
			for (String parentCategoryName : category.getCategoryNames()) {
				SiteCategory parentCategory = nameToCategory.get(parentCategoryName);
				if (categoryToNestedCategories.get(parentCategory) == null) {
					categoryToNestedCategories.put(parentCategory, new HashSet<SiteCategory>());
				}
				categoryToNestedCategories.get(parentCategory).add(category);
			}
		}

		List<SiteCategory> categories = new ArrayList<SiteCategory>(Arrays.asList(this.updateSite.getSite().getCategories()));
		categories.add(this.defaultCategory);
		// sort category so they are processed in reverse order of dependency
		// (Nested categories go first)
		Comparator<SiteCategory> isNestedCategoryComparator = new Comparator<SiteCategory>() {
			public int compare(SiteCategory category1, SiteCategory category2) {
				Set<SiteCategory> childrenOfCategory1 = categoryToNestedCategories.get(category1);
				Set<SiteCategory> childrenOfCategory2 = categoryToNestedCategories.get(category2);
				if (childrenOfCategory1 != null && childrenOfCategory1.contains(category2)) {
					// category2 nested in category1 => category2 < category1
					return +1;
				}
				if (childrenOfCategory2 != null && childrenOfCategory2.contains(category1)) {
					// category1 nested in category2 => category1 < category2
					return -1;
				}
				// Then recurse in childrenCategories for transitivity
				if (childrenOfCategory1 != null) {
					for (SiteCategory childOfCategory1 : childrenOfCategory1) {
						int res = this.compare(childOfCategory1, category2);
						if (res != 0) {
							return res;
						}
					}
				}
				if (childrenOfCategory2 != null) {
					for (SiteCategory childOfCategory2 : childrenOfCategory2) {
						int res = this.compare(category1, childOfCategory2);
						if (res != 0) {
							return res;
						}
					}
				}
				return 0;
			}
		};
		Collections.sort(categories, isNestedCategoryComparator);

		// Then create categories in the right order
		Map<String, IInstallableUnit> nameToCategoryIU = new HashMap<String, IInstallableUnit>();
		for (SiteCategory category : categories) {
			Set<IInstallableUnit> units = categoriesToIUs.get(category);
			if (units == null) {
				units = new HashSet<IInstallableUnit>();
			}
			Set<SiteCategory> nestedCategories = categoryToNestedCategories.get(category);
			if (nestedCategories != null) {
				for (SiteCategory nestedCategory : nestedCategories) {
					IInstallableUnit nestedCategoryIU = nameToCategoryIU.get(nestedCategory.getName());
					if (nestedCategoryIU != null) {
						units.add(nestedCategoryIU);
					}
				}
			}
			if (!units.isEmpty()) {
				IInstallableUnit categoryIU = createCategoryIU(category, units);
				result.addIU(categoryIU, IPublisherResult.NON_ROOT);
				nameToCategoryIU.put(category.getName(), categoryIU);
			}
		}
	}

	/**
	 * Creates an IU corresponding to an update site category
	 * @param category The category descriptor
	 * @param childrenIUs The IUs of the children that belong to the category (can be bundle, feature or nested categories)
	 * @param nestedCategory A nested category (optional)
	 * @return an IU representing the category
	 * @deprecated use {@link IInstallableUnit}{@link #createCategoryIU(SiteCategory, Set)} instead
	 */
	@Deprecated
	public IInstallableUnit createCategoryIU(SiteCategory category, Set<IInstallableUnit> childrenIUs, IInstallableUnit nestedCategory) {
		Set<IInstallableUnit> allIUs = new HashSet<IInstallableUnit>();
		if (childrenIUs != null) {
			allIUs.addAll(childrenIUs);
		}
		if (nestedCategory != null) {
			allIUs.add(nestedCategory);
		}
		return createCategoryIU(category, allIUs);
	}

	/**
	 * Creates an IU corresponding to an update site category
	 * @param category The category descriptor
	 * @param childrenIUs The IUs of the children that belong to the category (can be bundle, feature or nested categories)
	 * @return an IU representing the category
	 */
	public IInstallableUnit createCategoryIU(SiteCategory category, Set<IInstallableUnit> childrenIUs) {
		InstallableUnitDescription cat = new MetadataFactory.InstallableUnitDescription();
		cat.setSingleton(true);
		String categoryId = buildCategoryId(category.getName());
		cat.setId(categoryId);
		if (categoryVersion == null)
			cat.setVersion(Version.createOSGi(1, 0, 0, versionSuffixGenerator.generateSuffix(childrenIUs, Collections.<IVersionedId> emptyList())));
		else {
			if (categoryVersion.isOSGiCompatible()) {
				org.osgi.framework.Version osgiVersion = PublisherHelper.toOSGiVersion(categoryVersion);
				String qualifier = osgiVersion.getQualifier();
				if (qualifier.endsWith(QUALIFIER)) {
					String suffix = versionSuffixGenerator.generateSuffix(childrenIUs, Collections.<IVersionedId> emptyList());
					qualifier = qualifier.substring(0, qualifier.length() - 9) + suffix;
					categoryVersion = Version.createOSGi(osgiVersion.getMajor(), osgiVersion.getMinor(), osgiVersion.getMicro(), qualifier);
				}
			}
			cat.setVersion(categoryVersion);
		}

		String label = category.getLabel();
		cat.setProperty(IInstallableUnit.PROP_NAME, label != null ? label : category.getName());
		cat.setProperty(IInstallableUnit.PROP_DESCRIPTION, category.getDescription());

		ArrayList<IRequirement> reqsConfigurationUnits = new ArrayList<IRequirement>(childrenIUs.size());
		for (IInstallableUnit iu : childrenIUs) {
			VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
			reqsConfigurationUnits.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, iu.getFilter(), false, false));
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
