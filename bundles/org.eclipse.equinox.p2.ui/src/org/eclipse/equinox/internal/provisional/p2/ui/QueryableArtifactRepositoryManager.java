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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
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

	protected IRepositoryManager getRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	protected IRepository doLoadRepository(IRepositoryManager manager, URI location, IProgressMonitor monitor) throws ProvisionException {
		if (manager instanceof IArtifactRepositoryManager) {
			((IArtifactRepositoryManager) manager).loadRepository(location, monitor);
		}
		return null;
	}

	protected Collector query(URI uri, Query query, Collector collector, IProgressMonitor monitor) {
		// artifact repositories do not support querying, so we always use the location.
		query.perform(Arrays.asList(new URI[] {uri}).iterator(), collector);
		return collector;
	}
}
