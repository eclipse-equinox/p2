/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery.compatibility;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.discovery.*;
import org.eclipse.equinox.internal.p2.discovery.model.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * A strategy for discovering via installed platform {@link Bundle bundles}.
 * 
 * @author David Green
 */
public class BundleDiscoveryStrategy extends AbstractDiscoveryStrategy {

	public BundleDiscoveryStrategy() {
		// constructor
	}

	/**
	 * @throws CoreException - this exception is not actually thrown by the implementation,
	 * but is required by super contract.
	 */
	@Override
	public void performDiscovery(IProgressMonitor monitor) throws CoreException {
		if (items == null || categories == null) {
			throw new IllegalStateException();
		}
		IExtensionPoint extensionPoint = getExtensionRegistry().getExtensionPoint(ConnectorDiscoveryExtensionReader.EXTENSION_POINT_ID);
		IExtension[] extensions = extensionPoint.getExtensions();
		monitor.beginTask(Messages.BundleDiscoveryStrategy_task_loading_local_extensions, extensions.length == 0 ? 1 : extensions.length);
		try {
			if (extensions.length > 0) {
				processExtensions(new SubProgressMonitor(monitor, extensions.length), extensions);
			}
		} finally {
			monitor.done();
		}
	}

	protected void processExtensions(IProgressMonitor monitor, IExtension[] extensions) {
		monitor.beginTask(Messages.BundleDiscoveryStrategy_task_processing_extensions, extensions.length == 0 ? 1 : extensions.length);
		try {
			ConnectorDiscoveryExtensionReader extensionReader = new ConnectorDiscoveryExtensionReader();

			for (IExtension extension : extensions) {
				AbstractCatalogSource discoverySource = computeDiscoverySource(extension.getContributor());
				IConfigurationElement[] elements = extension.getConfigurationElements();
				for (IConfigurationElement element : elements) {
					if (monitor.isCanceled()) {
						return;
					}
					try {
						if (ConnectorDiscoveryExtensionReader.CONNECTOR_DESCRIPTOR.equals(element.getName())) {
							CatalogItem descriptor = extensionReader.readConnectorDescriptor(element, CatalogItem.class);
							descriptor.setSource(discoverySource);
							items.add(descriptor);
						} else if (ConnectorDiscoveryExtensionReader.CONNECTOR_CATEGORY.equals(element.getName())) {
							CatalogCategory category = extensionReader.readConnectorCategory(element, CatalogCategory.class);
							category.setSource(discoverySource);
							if (!discoverySource.getPolicy().isPermitCategories()) {
								LogHelper.log(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(Messages.BundleDiscoveryStrategy_categoryDisallowed, new Object[] {category.getName(), category.getId(), element.getContributor().getName()}), null));
							} else {
								categories.add(category);
							}
						} else if (ConnectorDiscoveryExtensionReader.CERTIFICATION.equals(element.getName())) {
							Certification certification = extensionReader.readCertification(element, Certification.class);
							certification.setSource(discoverySource);
							certifications.add(certification);
						} else {
							throw new ValidationException(NLS.bind(Messages.BundleDiscoveryStrategy_unexpected_element, element.getName()));
						}
					} catch (ValidationException e) {
						LogHelper.log(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(Messages.BundleDiscoveryStrategy_3, element.getContributor().getName(), e.getMessage()), e));
					}
				}
				monitor.worked(1);
			}

			tags.addAll(extensionReader.getTags());
		} finally {
			monitor.done();
		}
	}

	protected AbstractCatalogSource computeDiscoverySource(IContributor contributor) {
		Policy policy = new Policy(true);
		BundleDiscoverySource bundleDiscoverySource = new BundleDiscoverySource(Platform.getBundle(contributor.getName()));
		bundleDiscoverySource.setPolicy(policy);
		return bundleDiscoverySource;
	}

	protected IExtensionRegistry getExtensionRegistry() {
		return Platform.getExtensionRegistry();
	}

}
