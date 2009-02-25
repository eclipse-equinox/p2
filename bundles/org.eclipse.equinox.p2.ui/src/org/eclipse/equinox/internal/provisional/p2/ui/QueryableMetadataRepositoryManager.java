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
package org.eclipse.equinox.internal.provisional.p2.ui;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * An object that adds queryable support to a metadata repository 
 * manager.  It can be constructed to iterate over a specific array
 * of repositories named by URI, or filtered according to repository filter
 * flags.  When a query is provided, the object being queried is the loaded
 * repository, and collectors should be prepared to accept IInstallableUnits that
 * meet the query criteria.  Callers interested in only the resulting repository URI 
 * should specify a null query, in which case the collector will be accepting the URI's.
 */
public class QueryableMetadataRepositoryManager implements IQueryable {
	/**
	 * List<URI> of locations of repositories that were not found
	 */
	private ArrayList notFound = new ArrayList();

	/**
	 * Map<URI,IMetadataRepository> of loaded repositories.
	 */
	private HashMap loaded = new HashMap();

	private MultiStatus accumulatedNotFound = null;
	private boolean includeDisabledRepos;
	private Policy policy;

	public QueryableMetadataRepositoryManager(Policy policy, boolean includeDisabledRepos) {
		this.includeDisabledRepos = includeDisabledRepos;
		this.policy = policy;
	}

	/**
	 * Iterates over the metadata repositories configured in this queryable.
	 * If a query is specified, the query is run on each repository, passing any objects that satisfy the
	 * query to the provided collector.  If no query is specified, the repository URLs iterated are passed
	 * to the collector.
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor. 
	 * </p>
	 * 
	 * @param query The query to perform, or <code>null</code> if the repositories
	 * should not be loaded and queried.
	 * @param result Collects either the repository URLs (when the query is null), or the results 
	 *    of the query on each repository
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The collector argument
	 */
	public Collector query(Query query, Collector result, IProgressMonitor monitor) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			ProvUI.reportStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_NoRepositoryManager), StatusManager.SHOW | StatusManager.LOG);
			return result;
		}
		Collection repoLocations = getRepoLocations(manager);
		Iterator iterator = repoLocations.iterator();
		SubMonitor sub = SubMonitor.convert(monitor, ProvUIMessages.QueryableMetadataRepositoryManager_RepositoryQueryProgress, repoLocations.size() * 2);
		if (sub.isCanceled())
			return result;
		while (iterator.hasNext()) {
			URI location = (URI) iterator.next();
			if (sub.isCanceled())
				return result;
			if (query == null) {
				if (!result.accept(location))
					break;
				sub.worked(2);
			} else {
				try {
					Object alreadyLoaded = loaded.get(location);
					IMetadataRepository repo;
					if (alreadyLoaded == null) {
						repo = manager.loadRepository(location, sub.newChild(1));
					} else
						repo = (IMetadataRepository) alreadyLoaded;
					repo.query(query, result, sub.newChild(1));
				} catch (ProvisionException e) {
					if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
						handleNotFound(e, location);
					else
						ProvUI.handleException(e, NLS.bind(ProvUIMessages.ProvisioningUtil_LoadRepositoryFailure, location), StatusManager.LOG);
				} catch (OperationCanceledException e) {
					break;
				}
				reportAccumulatedStatus();
			}
		}
		return result;
	}

	/**
	 * Load all of the repositories referenced by this queryable.  This is an expensive operation.
	 * The status of any not found repositories is accumulated and must be reported manually
	 * using reportAccumulatedStatus()
	 * 
	 * @param monitor the progress monitor that should be used
	 */
	public void loadAll(IProgressMonitor monitor) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			ProvUI.reportStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_NoRepositoryManager), StatusManager.SHOW | StatusManager.LOG);
			return;
		}
		Collection repoLocations = getRepoLocations(manager);
		Iterator iter = repoLocations.iterator();
		SubMonitor sub = SubMonitor.convert(monitor, ProvUIMessages.QueryableMetadataRepositoryManager_RepositoryQueryProgress, repoLocations.size());
		if (sub.isCanceled())
			return;
		while (iter.hasNext()) {
			if (sub.isCanceled())
				return;
			URI location = (URI) iter.next();
			try {
				Object repo = loaded.get(location);
				if (repo == null) {
					SubMonitor mon = sub.newChild(1);
					mon.setTaskName(NLS.bind(ProvUIMessages.QueryableMetadataRepositoryManager_LoadRepositoryProgress, location.toString()));
					loaded.put(location, manager.loadRepository(location, mon));
				}
			} catch (ProvisionException e) {
				if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
					handleNotFound(e, location);
				else
					ProvUI.handleException(e, NLS.bind(ProvUIMessages.ProvisioningUtil_LoadRepositoryFailure, location), StatusManager.LOG);
			}
		}
	}

	/**
	 * Returns a Collection<URI> of repository locations.
	 */
	private Collection getRepoLocations(IMetadataRepositoryManager manager) {
		Set locations = new HashSet();
		int flags = policy.getQueryContext().getMetadataRepositoryFlags();
		locations.addAll(Arrays.asList(manager.getKnownRepositories(flags)));
		if (includeDisabledRepos) {
			locations.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED | flags)));
		}
		return locations;
	}

	private void handleNotFound(ProvisionException e, URI missingRepo) {
		// If we thought we had loaded it, get rid of the reference
		loaded.remove(missingRepo);
		// If we've already reported a URL is not found, don't report again.
		if (notFound.contains(missingRepo))
			return;
		// If someone else reported a URL is not found, don't report again.
		if (ProvUI.hasNotFoundStatusBeenReported(missingRepo)) {
			notFound.add(missingRepo);
			return;
		}
		notFound.add(missingRepo);
		ProvUI.notFoundStatusReported(missingRepo);
		// Empty multi statuses have a severity OK.  The platform status handler doesn't handle
		// this well.  We correct this by recreating a status with error severity
		// so that the platform status handler does the right thing.
		IStatus status = e.getStatus();
		if (status instanceof MultiStatus && ((MultiStatus) status).getChildren().length == 0)
			status = new Status(IStatus.ERROR, status.getPlugin(), status.getCode(), status.getMessage(), status.getException());
		if (accumulatedNotFound == null) {
			accumulatedNotFound = new MultiStatus(ProvUIActivator.PLUGIN_ID, ProvisionException.REPOSITORY_NOT_FOUND, new IStatus[] {status}, ProvUIMessages.QueryableMetadataRepositoryManager_MultipleRepositoriesNotFound, null);
		} else {
			accumulatedNotFound.add(status);
		}
	}

	public void reportAccumulatedStatus() {
		// If we've discovered not found repos we didn't know about, report them
		if (accumulatedNotFound != null) {
			// If there is only missing repo to report, use the specific message rather than the generic.
			if (accumulatedNotFound.getChildren().length == 1) {
				ProvUI.reportStatus(accumulatedNotFound.getChildren()[0], StatusManager.SHOW);
			} else {
				ProvUI.reportStatus(accumulatedNotFound, StatusManager.SHOW);
			}
		}
		// Reset the accumulated status so that next time we only report the newly not found repos.
		accumulatedNotFound = null;
	}

	/**
	 * Return a boolean indicating whether the repositories to be queried
	 * are already loaded.
	 * 
	 * @return <code>true</code> if all repositories to be queried by the
	 * receiver are loaded, <code>false</code> if they
	 * are not.
	 * 
	 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=229069
	 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=226343
	 */
	public boolean areRepositoriesLoaded() {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null || !(manager instanceof MetadataRepositoryManager)) {
			return false;
		}
		MetadataRepositoryManager mgr = (MetadataRepositoryManager) manager;
		Iterator repoURIs = getRepoLocations(mgr).iterator();
		while (repoURIs.hasNext()) {
			Object location = repoURIs.next();
			if (location instanceof URI) {
				IMetadataRepository repo = mgr.getRepository((URI) location);
				if (repo == null)
					return false;
			}
		}
		return true;
	}
}
