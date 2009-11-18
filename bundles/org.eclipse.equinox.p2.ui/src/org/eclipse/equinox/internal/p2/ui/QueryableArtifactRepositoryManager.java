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
package org.eclipse.equinox.internal.p2.ui;

import org.eclipse.equinox.p2.operations.RepositoryTracker;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.operations.ProvisioningSession;

/**
 * An object that adds provides query support for an artifact
 * repository manager.  The repository manipulator determines which 
 * repositories are included in the query.  Since artifact repositories
 * do not support queries, the query will be performed over the repository
 * locations.
 */
public class QueryableArtifactRepositoryManager extends QueryableRepositoryManager {

	public QueryableArtifactRepositoryManager(ProvisioningSession session, RepositoryTracker repositoryManipulator, boolean includeDisabledRepos) {
		super(session, repositoryManipulator, includeDisabledRepos);
	}

	protected URI[] getRepoLocations(IRepositoryManager manager) {
		Set locations = new HashSet();
		locations.addAll(Arrays.asList(manager.getKnownRepositories(repositoryFlags)));
		if (includeDisabledRepos) {
			locations.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED | repositoryFlags)));
		}
		return (URI[]) locations.toArray(new URI[locations.size()]);
	}

	protected IRepositoryManager getRepositoryManager() {
		return getSession().getArtifactRepositoryManager();
	}

	protected IRepository doLoadRepository(IRepositoryManager manager, URI location, IProgressMonitor monitor) throws ProvisionException {
		if (manager instanceof IArtifactRepositoryManager) {
			((IArtifactRepositoryManager) manager).loadRepository(location, monitor);
		}
		return null;
	}

	protected Collector query(URI[] uris, IQuery query, Collector collector, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, uris.length * 100);

		if (sub.isCanceled())
			return collector;
		// artifact repositories do not support querying, so we always use the location.
		query.perform(Arrays.asList(uris).iterator(), collector);

		return collector;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.QueryableRepositoryManager#getRepositoryFlags(org.eclipse.equinox.p2.ui.RepositoryManipulator)
	 */
	protected int getRepositoryFlags(RepositoryTracker repositoryManipulator) {
		return repositoryManipulator.getArtifactRepositoryFlags();
	}
}
