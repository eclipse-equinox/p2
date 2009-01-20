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
package org.eclipse.equinox.internal.provisional.p2.ui;

import java.net.URI;
import java.util.Arrays;
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
	 * If a query is specified, the query is run on each URI, passing any URIs that satisfy the
	 * query to the provided collector.  If no query is specified, all repository URIs iterated are passed
	 * to the collector.
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor. 
	 * </p>
	 * 
	 * @param query The query to perform on the URIs, or <code>null</code> if all URIs should
	 * be accepted.
	 * @param result Collects the repository URIs
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
		URI[] repoLocations = manager.getKnownRepositories(flags);
		if (monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask(ProvUIMessages.QueryableArtifactRepositoryManager_RepositoryQueryProgress, repoLocations.length);
		// If the query is null, all URI's are passed to the collector.
		// If it's a match query, matching URI's are passed to the collector.
		// Both cases require iteration over the repos.
		if (query == null || query instanceof IMatchQuery) {
			IMatchQuery isMatchQuery = (IMatchQuery) query;
			for (int i = 0; i < repoLocations.length; i++) {
				if (isMatchQuery == null || isMatchQuery.isMatch(repoLocations[i]))
					if (!result.accept(repoLocations[i]))
						break;
				monitor.worked(1);
			}
		} else
			// We don't know how to interpret this query, so just perform it over all of the URI's.
			query.perform(Arrays.asList(repoLocations).iterator(), result);

		monitor.done();
		return result;
	}
}
