/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
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

import org.eclipse.equinox.p2.core.ProvisionException;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.repository.helpers.AbstractRepositoryManager;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.MetadataRepositoryFactory;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;

/**
 * Default implementation of {@link IMetadataRepositoryManager}.
 */
public class MetadataRepositoryManager extends AbstractRepositoryManager<IInstallableUnit> implements IMetadataRepositoryManager {

	public MetadataRepositoryManager() {
		super();
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
		return factory.load(location, flags, monitor.newChild(10));
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

	public IStatus validateRepositoryLocation(URI location, IProgressMonitor monitor) {
		if (!location.isAbsolute())
			return new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_INVALID_LOCATION, NLS.bind(Messages.repoMan_relativeLocation, location.toString()), null);
		IMetadataRepository result = getRepository(location);
		if (result != null)
			return Status.OK_STATUS;
		String[] suffixes = getAllSuffixes();
		SubMonitor sub = SubMonitor.convert(monitor, Messages.repo_loading, suffixes.length * 100);
		IStatus status = new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.repoMan_notExists, location.toString()), null);
		for (int i = 0; i < suffixes.length; i++) {
			SubMonitor loopMonitor = sub.newChild(100);
			IExtension[] providers = findMatchingRepositoryExtensions(suffixes[i], null);
			// Loop over the candidates and return the first one that successfully loads
			loopMonitor.beginTask("", providers.length * 10); //$NON-NLS-1$
			for (int j = 0; j < providers.length; j++) {
				MetadataRepositoryFactory factory = (MetadataRepositoryFactory) createExecutableExtension(providers[j], EL_FACTORY);
				if (factory != null) {
					status = factory.validate(location, loopMonitor.newChild(10));
					if (status.isOK()) {
						sub.done();
						return status;
					}
				}
			}

		}
		sub.done();
		return status;
	}
}
