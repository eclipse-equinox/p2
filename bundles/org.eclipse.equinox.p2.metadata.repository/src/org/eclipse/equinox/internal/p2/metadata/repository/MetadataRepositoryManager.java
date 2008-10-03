/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.net.URL;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.AbstractRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.IMetadataRepositoryFactory;
import org.eclipse.osgi.util.NLS;

/**
 * Default implementation of {@link IMetadataRepositoryManager}.
 */
public class MetadataRepositoryManager extends AbstractRepositoryManager implements IMetadataRepositoryManager {

	public MetadataRepositoryManager() {
		super();
	}

	public void addRepository(IMetadataRepository repository) {
		super.addRepository(repository, true, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager#createRepository(java.net.URL, java.lang.String, java.lang.String, java.util.Map)
	 */
	public IMetadataRepository createRepository(URL location, String name, String type, Map properties) throws ProvisionException {
		Assert.isNotNull(name);
		Assert.isNotNull(type);
		boolean loaded = false;
		try {
			//repository should not already exist
			loadRepository(location, (IProgressMonitor) null, type);
			loaded = true;
		} catch (ProvisionException e) {
			//expected - fall through and create the new repository
		}
		if (loaded)
			fail(location, ProvisionException.REPOSITORY_EXISTS);

		IExtension extension = RegistryFactory.getRegistry().getExtension(Activator.REPO_PROVIDER_XPT, type);
		if (extension == null)
			fail(location, ProvisionException.REPOSITORY_UNKNOWN_TYPE);
		IMetadataRepositoryFactory factory = (IMetadataRepositoryFactory) createExecutableExtension(extension, EL_FACTORY);
		if (factory == null)
			fail(location, ProvisionException.REPOSITORY_FAILED_READ);
		IMetadataRepository result = factory.create(location, name, type, properties);
		if (result == null)
			fail(location, ProvisionException.REPOSITORY_FAILED_READ);
		clearNotFound(location);
		addRepository(result);
		return result;
	}

	protected IRepository factoryLoad(URL location, IExtension extension, SubMonitor monitor) throws ProvisionException {
		IMetadataRepositoryFactory factory = (IMetadataRepositoryFactory) createExecutableExtension(extension, EL_FACTORY);
		if (factory == null)
			return null;
		return factory.load(location, monitor.newChild(10));
	}

	protected String getBundleId() {
		return Activator.ID;
	}

	protected String getDefaultSuffix() {
		return "content.xml"; //$NON-NLS-1$
	}

	public IMetadataRepository getRepository(URL location) {
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

	public IMetadataRepository loadRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		return (IMetadataRepository) loadRepository(location, monitor, null);
	}

	/**
	 * Performs a query against all of the installable units of each known 
	 * repository, accumulating any objects that satisfy the query in the 
	 * provided collector.
	 * <p>
	 * Note that using this method can be quite expensive, as every known
	 * metadata repository will be loaded in order to query each one.  If a
	 * client wishes to query only certain repositories, it is better to use
	 * {@link #getKnownRepositories(int)} to filter the list of repositories
	 * loaded and then query each of the returned repositories.
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor. 
	 * 
	 * @param query The query to perform against each installable unit in each known repository
	 * @param collector Collects the results of the query
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The collector argument
	 */
	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		URL[] locations = getKnownRepositories(REPOSITORIES_ALL);
		SubMonitor sub = SubMonitor.convert(monitor, locations.length * 10);
		for (int i = 0; i < locations.length; i++) {
			try {
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				loadRepository(locations[i], sub.newChild(9)).query(query, collector, sub.newChild(1));
			} catch (ProvisionException e) {
				//ignore this repository for this query
			}
		}
		sub.done();
		return collector;
	}

	public IMetadataRepository refreshRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		return (IMetadataRepository) basicRefreshRepository(location, monitor);
	}

	public IStatus validateRepositoryLocation(URL location, IProgressMonitor monitor) {
		IMetadataRepository result = getRepository(location);
		if (result != null)
			return Status.OK_STATUS;
		String[] suffixes = getAllSuffixes();
		SubMonitor sub = SubMonitor.convert(monitor, Messages.repo_loading, suffixes.length * 100);
		IStatus status = new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.repoMan_notExists, location.toExternalForm()), null);
		for (int i = 0; i < suffixes.length; i++) {
			SubMonitor loopMonitor = sub.newChild(100);
			IExtension[] providers = findMatchingRepositoryExtensions(suffixes[i], null);
			// Loop over the candidates and return the first one that successfully loads
			loopMonitor.beginTask("", providers.length * 10); //$NON-NLS-1$
			for (int j = 0; j < providers.length; j++) {
				IMetadataRepositoryFactory factory = (IMetadataRepositoryFactory) createExecutableExtension(providers[j], EL_FACTORY);
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
