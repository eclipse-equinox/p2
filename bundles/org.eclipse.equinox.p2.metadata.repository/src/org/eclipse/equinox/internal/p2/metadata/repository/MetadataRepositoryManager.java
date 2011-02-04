/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.repository.helpers.AbstractRepositoryManager;
import org.eclipse.equinox.internal.p2.repository.helpers.LocationProperties;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory;

/**
 * Default implementation of {@link IMetadataRepositoryManager}.
 */
public class MetadataRepositoryManager extends AbstractRepositoryManager<IInstallableUnit> implements IMetadataRepositoryManager {

	public MetadataRepositoryManager(IProvisioningAgent agent) {
		super(agent);
	}

	public void addRepository(IMetadataRepository repository) {
		super.addRepository(repository, true, null);
	}

	public IMetadataRepository createRepository(URI location, String name, String type, Map<String, String> properties) throws ProvisionException {
		return (IMetadataRepository) doCreateRepository(location, name, type, properties);
	}

	protected IRepository<IInstallableUnit> factoryCreate(URI location, String name, String type, Map<String, String> properties, IExtension extension) throws ProvisionException {
		MetadataRepositoryFactory factory = (MetadataRepositoryFactory) createExecutableExtension(extension, EL_FACTORY);
		if (factory == null)
			return null;
		factory.setAgent(agent);
		return factory.create(location, name, type, properties);
	}

	protected IRepository<IInstallableUnit> factoryLoad(URI location, IExtension extension, int flags, SubMonitor monitor) throws ProvisionException {
		MetadataRepositoryFactory factory = (MetadataRepositoryFactory) createExecutableExtension(extension, EL_FACTORY);
		if (factory == null)
			return null;
		factory.setAgent(agent);
		return factory.load(location, flags, monitor);
	}

	protected String getBundleId() {
		return Activator.ID;
	}

	protected String getDefaultSuffix() {
		return "content.xml"; //$NON-NLS-1$
	}

	public IMetadataRepository getRepository(URI location) {
		return (IMetadataRepository) basicGetRepository(location);
	}

	protected String getRepositoryProviderExtensionPointId() {
		return Activator.REPO_PROVIDER_XPT;
	}

	protected String[] getPreferredRepositorySearchOrder(LocationProperties properties) {
		return properties.getMetadataFactorySearchOrder();
	}

	/**
	 * Restores metadata repositories specified as system properties.
	 */
	protected String getRepositorySystemProperty() {
		return "eclipse.p2.metadataRepository"; //$NON-NLS-1$
	}

	protected int getRepositoryType() {
		return IRepository.TYPE_METADATA;
	}

	public IMetadataRepository loadRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
		return loadRepository(location, 0, monitor);
	}

	public IMetadataRepository loadRepository(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
		return (IMetadataRepository) loadRepository(location, monitor, null, flags);
	}

	public IMetadataRepository refreshRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
		return (IMetadataRepository) basicRefreshRepository(location, monitor);
	}

}
