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
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Query;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IUViewQueryContext;

/**
 * An object that adds provides queryable support 
 * for an artifact repository manager.  The query context determines which 
 * repositories are included in the query.  Since artifact repositories
 * do not support queries, the query will be performed over the repository
 * locations.
 */
public class QueryableArtifactRepositoryManager extends QueryableRepositoryManager {

	public QueryableArtifactRepositoryManager(IUViewQueryContext queryContext, boolean includeDisabledRepos) {
		super(queryContext, includeDisabledRepos);
	}

	protected URI[] getRepoLocations(IRepositoryManager manager) {
		Set locations = new HashSet();
		int flags = queryContext.getArtifactRepositoryFlags();
		locations.addAll(Arrays.asList(manager.getKnownRepositories(flags)));
		if (includeDisabledRepos) {
			locations.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED | flags)));
		}
		return (URI[]) locations.toArray(new URI[locations.size()]);
	}

	protected IRepositoryManager getRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	protected IRepository doLoadRepository(IRepositoryManager manager, URI location, IProgressMonitor monitor) throws ProvisionException {
		if (manager instanceof IArtifactRepositoryManager) {
			((IArtifactRepositoryManager) manager).loadRepository(location, monitor);
		}
		return null;
	}

	protected Collector query(URI[] uris, Query query, Collector collector, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, uris.length * 100);

		if (sub.isCanceled())
			return collector;
		// artifact repositories do not support querying, so we always use the location.
		query.perform(Arrays.asList(uris).iterator(), collector);

		return collector;
	}
}
