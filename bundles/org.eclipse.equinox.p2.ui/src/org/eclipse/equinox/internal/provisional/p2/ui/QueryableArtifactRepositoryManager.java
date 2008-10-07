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

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.*;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * An object that adds queryable support to an artifact repository 
 * manager.  It can be constructed to filter the repositories according to repository filter
 * flags.  When a query is provided, the object being queried is repository URL.
 * Callers interested in only the resulting repository URL's can specify a null query, 
 * in which case the collector will be accepting all iterated URL's.
 */
public class QueryableArtifactRepositoryManager implements IQueryable {

	int flags = IRepositoryManager.REPOSITORIES_ALL;

	public QueryableArtifactRepositoryManager(int flags) {
		this.flags = flags;
	}

	/**
	 * Iterates over the artifact repositories configured in this queryable.
	 * If a query is specified, the query is run on each URL, passing any URLs that satisfy the
	 * query to the provided collector.  If no query is specified, all repository URLs iterated are passed
	 * to the collector.
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor. 
	 * </p>
	 * 
	 * @param query The query to perform on the URLs, or <code>null</code> if all URLs should
	 * be accepted.
	 * @param result Collects the repository URLs
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The collector argument
	 */
	public Collector query(Query query, Collector result, IProgressMonitor monitor) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			ProvUI.reportStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_NoRepositoryManager), StatusManager.SHOW | StatusManager.LOG);
			return result;
		}
		URL[] repoURLs = manager.getKnownRepositories(flags);
		if (monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask(ProvUIMessages.QueryableArtifactRepositoryManager_RepositoryQueryProgress, repoURLs.length);
		for (int i = 0; i < repoURLs.length; i++) {
			if (query == null || query.isMatch(repoURLs[i]))
				result.accept(repoURLs[i]);
			monitor.worked(1);
		}
		monitor.done();
		return result;
	}
}
