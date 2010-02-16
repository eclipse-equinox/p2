/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.discovery.repository;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.TranslationSupport;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.MatchQuery;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * @author Steffen Pingel
 */
public class RepositoryDiscoveryStrategy extends AbstractDiscoveryStrategy {

	private static final String IU_PROPERTY_CATEGORY = "org.eclipse.equinox.p2.type.category"; //$NON-NLS-1$

	private static final String PLUGIN_ID = "org.eclipse.equinox.p2.discovery.repository"; //$NON-NLS-1$

	private static final String P2_FEATURE_GROUP_SUFFIX = ".feature.group"; //$NON-NLS-1$

	private final List<URI> locations;

	private final Map<IMetadataRepository, RepositorySource> sourceByRepository;

	private final Map<String, CatalogCategory> categoryById;

	private final Map<String, CatalogItem> catalogItemById;

	public RepositoryDiscoveryStrategy() {
		this.locations = new ArrayList<URI>();
		this.sourceByRepository = new HashMap<IMetadataRepository, RepositorySource>();
		this.categoryById = new HashMap<String, CatalogCategory>();
		this.catalogItemById = new HashMap<String, CatalogItem>();
	}

	public void addLocation(URI location) {
		locations.add(location);
	}

	public void removeLocation(URI location) {
		locations.remove(location);
	}

	@Override
	public void performDiscovery(IProgressMonitor progressMonitor) throws CoreException {
		// ignore
		SubMonitor monitor = SubMonitor.convert(progressMonitor);
		monitor.setWorkRemaining(100);
		try {
			List<IMetadataRepository> repositories = addRepositories(monitor.newChild(50));
			queryInstallableUnits(monitor.newChild(50), repositories);
			connectCategories();
		} catch (ProvisionException e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, "Failed to process repository contents", e));
		}
	}

	@SuppressWarnings("restriction")
	private void connectCategories() {
		for (CatalogCategory category : categories) {
			if (category.getData() instanceof IInstallableUnit) {
				IInstallableUnit categoryIU = (IInstallableUnit) category.getData();
				Collection<IRequirement> required = categoryIU.getRequiredCapabilities();
				for (IRequirement requirement : required) {
					if (requirement instanceof IRequiredCapability) {
						IRequiredCapability capability = (IRequiredCapability) requirement;
						CatalogItem item = catalogItemById.get(capability.getName());
						if (item != null) {
							item.setCategoryId(category.getId());
						}
					}
				}
			}
		}
	}

	private List<IMetadataRepository> addRepositories(SubMonitor monitor) throws ProvisionException {
		ProvisioningSession session = ProvisioningUI.getDefaultUI().getSession();

		monitor.setWorkRemaining(locations.size());

		RepositoryTracker repositoryTracker = ProvisioningUI.getDefaultUI().getRepositoryTracker();
		for (URI location : locations) {
			repositoryTracker.addRepository(location, null, session);
			monitor.worked(1);
		}

		// fetch meta-data for these repositories
		ArrayList<IMetadataRepository> repositories = new ArrayList<IMetadataRepository>();
		for (URI uri : locations) {
			IMetadataRepository repository = session.getMetadataRepositoryManager().loadRepository(uri,
					monitor.newChild(1));
			repositories.add(repository);
		}
		return repositories;
	}

	private void checkCancelled(IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	private void queryInstallableUnits(SubMonitor monitor, List<IMetadataRepository> repositories) {
		monitor.setWorkRemaining(repositories.size());
		for (final IMetadataRepository repository : repositories) {
			checkCancelled(monitor);
			IQuery<IInstallableUnit> query = new MatchQuery<IInstallableUnit>() {
				@Override
				public boolean isMatch(IInstallableUnit candidate) {
					if (isQualifyingFeature(candidate)) {
						process(repository, candidate);
						return true;
					}
					return false;
				}
			};
			repository.query(query, monitor.newChild(1));
		}
	}

	protected void process(IMetadataRepository repository, IInstallableUnit candidate) {
		if (isCategory(candidate)) {
			processCategory(repository, candidate);
		} else {
			processCatalogItem(repository, candidate);
		}
	}

	private CatalogItem processCatalogItem(IMetadataRepository repository, IInstallableUnit candidate) {
		CatalogItem item = catalogItemById.get(candidate.getId());
		if (item != null) {
			return item;
		}

		item = new CatalogItem();
		item.setId(candidate.getId());
		item.setDescription(getProperty(candidate, IInstallableUnit.PROP_DESCRIPTION));
		item.setName(getProperty(candidate, IInstallableUnit.PROP_NAME));
		item.setProvider(getProperty(candidate, IInstallableUnit.PROP_PROVIDER));
		item.setSource(getSource(repository));
		item.setData(candidate);
		item.setSiteUrl(repository.getLocation().toString());
		item.getInstallableUnits().add(item.getId());

		catalogItemById.put(item.getId(), item);
		items.add(item);
		return item;
	}

	public String getProperty(IInstallableUnit candidate, String key) {
		String value = TranslationSupport.getInstance().getIUProperty(candidate, key);
		return (value != null) ? value : "";
	}

	private AbstractCatalogSource getSource(IMetadataRepository repository) {
		RepositorySource source = sourceByRepository.get(repository);
		if (source == null) {
			source = new RepositorySource(repository);
			sourceByRepository.put(repository, source);
		}
		return source;
	}

	private CatalogCategory processCategory(IMetadataRepository repository, IInstallableUnit candidate) {
		CatalogCategory category = categoryById.get(candidate.getId());
		if (category != null) {
			return category;
		}

		category = new CatalogCategory();
		category.setId(candidate.getId());
		category.setDescription(getProperty(candidate, IInstallableUnit.PROP_DESCRIPTION));
		category.setName(getProperty(candidate, IInstallableUnit.PROP_NAME));
		category.setSource(getSource(repository));
		category.setData(candidate);

		categoryById.put(category.getId(), category);
		categories.add(category);
		return category;
	}

	private boolean isQualifyingFeature(IInstallableUnit candidate) {
		if (candidate.getId().endsWith(P2_FEATURE_GROUP_SUFFIX)) {
			Collection<IProvidedCapability> providedCapabilities = candidate.getProvidedCapabilities();
			if (providedCapabilities != null && providedCapabilities.size() > 0) {
				for (IProvidedCapability capability : providedCapabilities) {
					if ("org.eclipse.equinox.p2.iu".equals(capability.getNamespace())) { //$NON-NLS-1$
						String name = capability.getName();
						if (name.endsWith(P2_FEATURE_GROUP_SUFFIX)) {
							return true;
						}
					}
				}
			}
		} else if (isCategory(candidate)) {
			return true;
		}
		return false;
	}

	private Boolean isCategory(IInstallableUnit candidate) {
		return Boolean.valueOf(candidate.getProperty(IU_PROPERTY_CATEGORY));
	}

}
