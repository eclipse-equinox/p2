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
package org.eclipse.equinox.internal.provisional.p2.ui.query;

import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.*;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.model.MetadataRepositories;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * An object that adds queryable support to a metadata repository 
 * manager.  It can be constructed to iterate over a specific array
 * of repositories named by URL, or filtered according to repository filter
 * flags.  When a query is provided, the object being queried is the loaded
 * repository, and collectors should be prepared to accept IInstallableUnits that
 * meet the query criteria.  Callers interested in only the resulting repository URL's 
 * should specify a null query, in which case the collector will be accepting the URL's.
 */
public class QueryableMetadataRepositoryManager implements IQueryable {
	private MetadataRepositories repositories;
	private ArrayList notFound = new ArrayList();
	private MultiStatus accumulatedNotFound = null;

	public QueryableMetadataRepositoryManager(MetadataRepositories repositories) {
		this.repositories = repositories;
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
		List repoURLs = getRepoLocations(manager);

		SubMonitor sub = SubMonitor.convert(monitor, ProvUIMessages.QueryableMetadataRepositoryManager_RepositoryQueryProgress, repoURLs.size() * 2);
		if (sub.isCanceled())
			return result;
		for (int i = 0; i < repoURLs.size(); i++) {
			if (sub.isCanceled())
				return result;
			if (query == null) {
				result.accept(repoURLs.get(i));
				sub.worked(2);
			} else {
				URL url = (URL) repoURLs.get(i);
				try {
					IMetadataRepository repo = manager.loadRepository(url, sub.newChild(1));
					repo.query(query, result, sub.newChild(1));
				} catch (ProvisionException e) {
					if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
						handleNotFound(e, url);
					else
						ProvUI.handleException(e, NLS.bind(ProvUIMessages.ProvisioningUtil_LoadRepositoryFailure, repoURLs.get(i)), StatusManager.LOG);
				}
				reportAccumulatedStatus();

			}
		}
		return result;
	}

	/**
	 * Load all of the repositories referenced by this queryable.  This is an expensive operation.
	 * @param monitor the progress monitor that should be used
	 */
	public void loadAll(IProgressMonitor monitor) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			ProvUI.reportStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_NoRepositoryManager), StatusManager.SHOW | StatusManager.LOG);
			return;
		}
		List repoURLs = getRepoLocations(manager);
		SubMonitor sub = SubMonitor.convert(monitor, ProvUIMessages.QueryableMetadataRepositoryManager_RepositoryQueryProgress, repoURLs.size() * 2);
		if (sub.isCanceled())
			return;
		for (int i = 0; i < repoURLs.size(); i++) {
			if (sub.isCanceled())
				return;
			URL url = (URL) repoURLs.get(i);
			try {
				manager.loadRepository(url, sub.newChild(1));
			} catch (ProvisionException e) {
				if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
					handleNotFound(e, url);
				else
					ProvUI.handleException(e, NLS.bind(ProvUIMessages.ProvisioningUtil_LoadRepositoryFailure, repoURLs.get(i)), StatusManager.LOG);
			}
		}
		reportAccumulatedStatus();

	}

	private List getRepoLocations(IMetadataRepositoryManager manager) {
		ArrayList locations = new ArrayList();
		if (repositories.getMetadataRepositories() != null) {
			locations.addAll(Arrays.asList(repositories.getMetadataRepositories()));
		} else {
			locations.addAll(Arrays.asList(manager.getKnownRepositories(repositories.getRepoFlags())));
			if (repositories.getIncludeDisabledRepositories()) {
				locations.addAll(Arrays.asList(manager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_DISABLED)));
			}
		}
		return locations;
	}

	private void handleNotFound(ProvisionException e, URL missingRepo) {
		// If we've already reported a URL is not found, don't report again.
		if (notFound.contains(missingRepo.toExternalForm()))
			return;
		notFound.add(missingRepo.toExternalForm());
		if (accumulatedNotFound == null) {
			accumulatedNotFound = new MultiStatus(ProvUIActivator.PLUGIN_ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(ProvUIMessages.ProvisioningUtil_RepositoryNotFound, missingRepo.toExternalForm()), null);
		}
		accumulatedNotFound.add(e.getStatus());
	}

	private void reportAccumulatedStatus() {
		// If we've discovered not found repos we didn't know about, report them
		if (accumulatedNotFound != null) {
			// If we didn't find several repos in one pass, use a more generic top level message.
			if (accumulatedNotFound.getChildren().length > 1) {
				MultiStatus multiples = new MultiStatus(ProvUIActivator.PLUGIN_ID, ProvisionException.REPOSITORY_NOT_FOUND, "Some repositories could not be found.  Check the details.", null);
				multiples.addAll(accumulatedNotFound);
				accumulatedNotFound = multiples;

			}
			ProvUI.reportStatus(accumulatedNotFound, StatusManager.SHOW);
		}
		// Reset the accumulated status so that next time we only report the newly not found repos.
		accumulatedNotFound = null;
	}
}
