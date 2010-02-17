/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.discovery.model.AbstractCatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Certification;
import org.eclipse.equinox.internal.p2.discovery.model.FeatureFilter;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

/**
 * A means of discovering connectors.
 * 
 * @author David Green
 */
public class Catalog {

	private List<CatalogItem> items = Collections.emptyList();

	private List<CatalogCategory> categories = Collections.emptyList();

	private List<Certification> certifications = Collections.emptyList();

	private List<CatalogItem> filteredItems = Collections.emptyList();

	private final List<AbstractDiscoveryStrategy> discoveryStrategies = new ArrayList<AbstractDiscoveryStrategy>();

	private List<Tag> tags = Collections.emptyList();

	private Dictionary<Object, Object> environment = System.getProperties();

	private boolean verifyUpdateSiteAvailability = false;

	private Map<String, Version> featureToVersion = null;

	public Catalog() {
	}

	/**
	 * get the discovery strategies to use.
	 */
	public List<AbstractDiscoveryStrategy> getDiscoveryStrategies() {
		return discoveryStrategies;
	}

	/**
	 * Initialize this by performing discovery. Discovery may take a long time as it involves network access.
	 * PRECONDITION: must add at least one {@link #getDiscoveryStrategies() discovery strategy} prior to calling.
	 * 
	 * @return
	 */
	public IStatus performDiscovery(IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus(DiscoveryCore.ID_PLUGIN, 0,
				Messages.Catalog_Failed_to_discovery_all_Error, null);
		if (discoveryStrategies.isEmpty()) {
			throw new IllegalStateException();
		}
		List<CatalogItem> items = new ArrayList<CatalogItem>();
		List<CatalogCategory> categories = new ArrayList<CatalogCategory>();
		List<Certification> certifications = new ArrayList<Certification>();
		List<Tag> tags = new ArrayList<Tag>();

		final int totalTicks = 100000;
		final int discoveryTicks = totalTicks - (totalTicks / 10);
		monitor.beginTask(Messages.Catalog_task_discovering_connectors, totalTicks);
		try {
			for (AbstractDiscoveryStrategy discoveryStrategy : discoveryStrategies) {
				if (monitor.isCanceled()) {
					status.add(Status.CANCEL_STATUS);
					break;
				}
				discoveryStrategy.setCategories(categories);
				discoveryStrategy.setItems(items);
				discoveryStrategy.setCertifications(certifications);
				discoveryStrategy.setTags(tags);
				try {
					discoveryStrategy.performDiscovery(new SubProgressMonitor(monitor, discoveryTicks
							/ discoveryStrategies.size()));
				} catch (CoreException e) {
					status.add(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(
							Messages.Catalog_Strategy_failed_Error, discoveryStrategy.getClass().getSimpleName()), e));
				}
			}

			update(categories, items, certifications, tags);
		} finally {
			monitor.done();
		}
		return status;
	}

	protected void update(List<CatalogCategory> categories, List<CatalogItem> items,
			List<Certification> certifications, List<Tag> tags) {
		this.categories = categories;
		this.items = items;
		this.certifications = certifications;
		this.tags = tags;
		this.filteredItems = new ArrayList<CatalogItem>();

		filterDescriptors();
		connectCategoriesToDescriptors();
		connectCertificationsToDescriptors();
	}

	/**
	 * get the top-level categories
	 * 
	 * @return the categories, or an empty list if there are none.
	 */
	public List<CatalogCategory> getCategories() {
		return categories;
	}

	/**
	 * get the connectors that were discovered and not filtered
	 * 
	 * @return the connectors, or an empty list if there are none.
	 */
	public List<CatalogItem> getItems() {
		return items;
	}

	public List<Tag> getTags() {
		return tags;
	}

	/**
	 * get the connectors that were discovered but filtered
	 * 
	 * @return the filtered connectors, or an empty list if there were none.
	 */
	public List<CatalogItem> getFilteredItems() {
		return filteredItems;
	}

	/**
	 * get a list of known certifications
	 * 
	 * @return the certifications, or an ampty list if there are none.
	 */
	public List<Certification> getCertifications() {
		return certifications;
	}

	/**
	 * The environment used to resolve {@link AbstractCatalogItem#getPlatformFilter() platform filters}. Defaults to the
	 * current environment.
	 */
	public Dictionary<Object, Object> getEnvironment() {
		return environment;
	}

	/**
	 * The environment used to resolve {@link AbstractCatalogItem#getPlatformFilter() platform filters}. Defaults to the
	 * current environment.
	 */
	public void setEnvironment(Dictionary<Object, Object> environment) {
		if (environment == null) {
			throw new IllegalArgumentException();
		}
		this.environment = environment;
	}

	/**
	 * indicate if update site availability should be verified. The default is false.
	 * 
	 * @see CatalogItem#getAvailable()
	 * @see #verifySiteAvailability(IProgressMonitor)
	 */
	public boolean isVerifyUpdateSiteAvailability() {
		return verifyUpdateSiteAvailability;
	}

	/**
	 * indicate if update site availability should be verified. The default is false.
	 * 
	 * @see CatalogItem#getAvailable()
	 * @see #verifySiteAvailability(IProgressMonitor)
	 */
	public void setVerifyUpdateSiteAvailability(boolean verifyUpdateSiteAvailability) {
		this.verifyUpdateSiteAvailability = verifyUpdateSiteAvailability;
	}

	/**
	 * <em>not for general use: public for testing purposes only</em> A map of installed features to their version. Used
	 * to resolve {@link AbstractCatalogItem#getFeatureFilter() feature filters}.
	 */
	public Map<String, Version> getFeatureToVersion() {
		return featureToVersion;
	}

	/**
	 * <em>not for general use: public for testing purposes only</em> A map of installed features to their version. Used
	 * to resolve {@link AbstractCatalogItem#getFeatureFilter() feature filters}.
	 */
	public void setFeatureToVersion(Map<String, Version> featureToVersion) {
		this.featureToVersion = featureToVersion;
	}

	private void connectCertificationsToDescriptors() {
		Map<String, Certification> idToCertification = new HashMap<String, Certification>();
		for (Certification certification : certifications) {
			Certification previous = idToCertification.put(certification.getId(), certification);
			if (previous != null) {
				LogHelper.log(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(
						"Duplicate certification id ''{0}'': declaring sources: {1}, {2}", //$NON-NLS-1$
						new Object[] { certification.getId(), certification.getSource().getId(),
								previous.getSource().getId() })));
			}
		}

		for (CatalogItem connector : items) {
			if (connector.getCertificationId() != null) {
				Certification certification = idToCertification.get(connector.getCertificationId());
				if (certification != null) {
					connector.setCertification(certification);
				} else {
					LogHelper.log(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(
							"Unknown category ''{0}'' referenced by connector ''{1}'' declared in {2}", new Object[] { //$NON-NLS-1$
							connector.getCertificationId(), connector.getId(), connector.getSource().getId() })));
				}
			}
		}
	}

	private void connectCategoriesToDescriptors() {
		Map<String, CatalogCategory> idToCategory = new HashMap<String, CatalogCategory>();
		for (CatalogCategory category : categories) {
			CatalogCategory previous = idToCategory.put(category.getId(), category);
			if (previous != null) {
				LogHelper.log(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(
						Messages.Catalog_duplicate_category_id, new Object[] { category.getId(),
								category.getSource().getId(), previous.getSource().getId() })));
			}
		}

		for (CatalogItem connector : items) {
			CatalogCategory category = idToCategory.get(connector.getCategoryId());
			if (category != null) {
				category.getItems().add(connector);
				connector.setCategory(category);
			} else {
				LogHelper.log(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(
						Messages.Catalog_bundle_references_unknown_category, new Object[] { connector.getCategoryId(),
								connector.getId(), connector.getSource().getId() })));
			}
		}
	}

	/**
	 * eliminate any connectors whose {@link AbstractCatalogItem#getPlatformFilter() platform filters} don't match
	 */
	private void filterDescriptors() {
		for (CatalogItem connector : new ArrayList<CatalogItem>(items)) {
			if (connector.getPlatformFilter() != null && connector.getPlatformFilter().trim().length() > 0) {
				boolean match = false;
				try {
					Filter filter = FrameworkUtil.createFilter(connector.getPlatformFilter());
					match = filter.match(environment);
				} catch (InvalidSyntaxException e) {
					LogHelper.log(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(
							Messages.Catalog_illegal_filter_syntax, new Object[] { connector.getPlatformFilter(),
									connector.getId(), connector.getSource().getId() })));
				}
				if (!match) {
					items.remove(connector);
					filteredItems.add(connector);
				}
			}
			for (FeatureFilter featureFilter : connector.getFeatureFilter()) {
				if (featureToVersion == null) {
					featureToVersion = computeFeatureToVersion();
				}
				boolean match = false;
				Version version = featureToVersion.get(featureFilter.getFeatureId());
				if (version != null) {
					VersionRange versionRange = new VersionRange(featureFilter.getVersion());
					if (versionRange.isIncluded(version)) {
						match = true;
					}
				}
				if (!match) {
					items.remove(connector);
					filteredItems.add(connector);
					break;
				}
			}
		}
	}

	private Map<String, Version> computeFeatureToVersion() {
		Map<String, Version> featureToVersion = new HashMap<String, Version>();
		for (IBundleGroupProvider provider : Platform.getBundleGroupProviders()) {
			for (IBundleGroup bundleGroup : provider.getBundleGroups()) {
				for (Bundle bundle : bundleGroup.getBundles()) {
					featureToVersion.put(bundle.getSymbolicName(), bundle.getVersion());
				}
			}
		}
		return featureToVersion;
	}

	public void dispose() {
		for (final AbstractDiscoveryStrategy strategy : discoveryStrategies) {
			SafeRunner.run(new ISafeRunnable() {

				public void run() throws Exception {
					strategy.dispose();
				}

				public void handleException(Throwable exception) {
					LogHelper.log(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN,
							Messages.Catalog_exception_disposing + strategy.getClass().getName(), exception));
				}
			});
		}
	}

	public void setTags(List<Tag> tags) {
		this.tags = new ArrayList<Tag>(tags);
	}

}
