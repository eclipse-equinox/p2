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

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.repository.helpers.AbstractRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.MetadataRepositoryFactory;
import org.eclipse.equinox.p2.metadata.query.IQuery;
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

	public IMetadataRepository createRepository(URI location, String name, String type, Map properties) throws ProvisionException {
		return (IMetadataRepository) doCreateRepository(location, name, type, properties);
	}

	protected IRepository factoryCreate(URI location, String name, String type, Map properties, IExtension extension) throws ProvisionException {
		MetadataRepositoryFactory factory = (MetadataRepositoryFactory) createExecutableExtension(extension, EL_FACTORY);
		if (factory == null)
			return null;
		return factory.create(location, name, type, properties);
	}

	protected IRepository factoryLoad(URI location, IExtension extension, int flags, SubMonitor monitor) throws ProvisionException {
		MetadataRepositoryFactory factory = (MetadataRepositoryFactory) createExecutableExtension(extension, EL_FACTORY);
		if (factory == null)
			return null;
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

	/**
	 * @deprecated see {@link #loadRepository(URI, int, IProgressMonitor)}
	 */
	public IMetadataRepository loadRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
		return loadRepository(location, 0, monitor);
	}

	public IMetadataRepository loadRepository(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
		return (IMetadataRepository) loadRepository(location, monitor, null, flags);
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
	public Collector query(IQuery query, Collector collector, IProgressMonitor monitor) {
		URI[] locations = getKnownRepositories(REPOSITORIES_ALL);
		List queryables = new ArrayList(locations.length); // use a list since we don't know exactly how many will load
		SubMonitor sub = SubMonitor.convert(monitor, locations.length * 10);
		for (int i = 0; i < locations.length; i++) {
			try {
				if (sub.isCanceled())
					throw new OperationCanceledException();
				queryables.add(loadRepository(locations[i], sub.newChild(9)));
			} catch (ProvisionException e) {
				//ignore this repository for this query
			}
		}
		try {
			IQueryable[] queryablesArray = (IQueryable[]) queryables.toArray(new IQueryable[queryables.size()]);
			CompoundQueryable compoundQueryable = new CompoundQueryable(queryablesArray);
			compoundQueryable.query(query, collector, sub.newChild(locations.length * 1));
		} finally {
			sub.done();
		}
		return collector;
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
