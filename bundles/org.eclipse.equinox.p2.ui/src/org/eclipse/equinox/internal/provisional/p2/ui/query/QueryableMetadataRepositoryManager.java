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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.*;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
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
	private URL[] metadataRepositories;
	private int flags = IMetadataRepositoryManager.REPOSITORIES_ALL;

	public QueryableMetadataRepositoryManager(URL[] metadataRepositories) {
		this.metadataRepositories = metadataRepositories;
	}

	public QueryableMetadataRepositoryManager(int flags) {
		this.flags = flags;
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
	 * @param collector Collects either the repository URLs (when the query is null), or the results 
	 *    of the query on each repository
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The collector argument
	 */
	public Collector query(Query query, Collector result, IProgressMonitor monitor) {
		URL[] repoURLs;
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			ProvUI.reportStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_NoRepositoryManager), StatusManager.SHOW | StatusManager.LOG);
			return result;
		}

		if (metadataRepositories != null) {
			repoURLs = metadataRepositories;
		} else {
			repoURLs = manager.getKnownRepositories(flags);
		}
		SubMonitor sub = SubMonitor.convert(monitor, ProvUIMessages.QueryableMetadataRepositoryManager_RepositoryQueryProgress, repoURLs.length * 2);
		for (int i = 0; i < repoURLs.length; i++) {
			if (query == null) {
				result.accept(repoURLs[i]);
				sub.worked(2);
			} else {
				try {
					IMetadataRepository repo = manager.loadRepository(repoURLs[i], sub.newChild(1));
					repo.query(query, result, sub.newChild(1));
				} catch (ProvisionException e) {
					ProvUI.handleException(e, NLS.bind(ProvUIMessages.ProvisioningUtil_LoadRepositoryFailure, repoURLs[i]), StatusManager.LOG);
				}
			}
		}
		return result;
	}
}
