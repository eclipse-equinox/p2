/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.ui.RepositoryManipulator;

/**
 * An object that adds provides specialized/optimized queryable support 
 * for a metadata repository.  The repository manipulator determines which repositories 
 * are included in the query.  Callers interested in only the resulting repository URIs
 * should specify a {@link RepositoryLocationQuery}, in which case the 
 * query is performed over the URI's.  Otherwise the query is performed over
 * the repositories themselves.
 */
public class QueryableMetadataRepositoryManager extends QueryableRepositoryManager {

	public QueryableMetadataRepositoryManager(ProvisioningSession session, RepositoryManipulator manipulator, boolean includeDisabledRepos) {
		super(session, manipulator, includeDisabledRepos);
	}

	protected IRepository getRepository(IRepositoryManager manager, URI location) {
		// note the use of MetadataRepositoryManager (the concrete implementation).
		if (manager instanceof MetadataRepositoryManager) {
			return ((MetadataRepositoryManager) manager).getRepository(location);
		}
		return super.getRepository(manager, location);
	}

	protected IRepositoryManager getRepositoryManager() {
		return getSession().getMetadataRepositoryManager();
	}

	protected IRepository doLoadRepository(IRepositoryManager manager, URI location, IProgressMonitor monitor) throws ProvisionException {
		if (manager instanceof IMetadataRepositoryManager) {
			return getSession().loadMetadataRepository(location, monitor);
		}
		return null;
	}

	protected Collector query(URI uris[], IQuery query, Collector collector, IProgressMonitor monitor) {
		if (query instanceof RepositoryLocationQuery) {
			query.perform(Arrays.asList(uris).iterator(), collector);
			monitor.done();
		} else {
			SubMonitor sub = SubMonitor.convert(monitor, (uris.length + 1) * 100);
			ArrayList loadedRepos = new ArrayList(uris.length);
			for (int i = 0; i < uris.length; i++) {
				IRepository repo = null;
				try {
					repo = loadRepository(getRepositoryManager(), uris[i], sub.newChild(100));
				} catch (ProvisionException e) {
					handleLoadFailure(e, uris[i]);
				} catch (OperationCanceledException e) {
					// user has canceled
					repo = null;
				}
				if (repo != null)
					loadedRepos.add(repo);
			}
			if (loadedRepos.size() > 0) {
				IQueryable[] queryables = (IQueryable[]) loadedRepos.toArray(new IQueryable[loadedRepos.size()]);
				collector = new CompoundQueryable(queryables).query(query, collector, sub.newChild(100));
			}
		}
		return collector;
	}

	protected URI[] getRepoLocations(IRepositoryManager manager) {
		Set locations = new HashSet();
		locations.addAll(Arrays.asList(manager.getKnownRepositories(repositoryFlags)));
		if (includeDisabledRepos) {
			locations.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED | repositoryFlags)));
		}
		return (URI[]) locations.toArray(new URI[locations.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.QueryableRepositoryManager#getRepositoryFlags(org.eclipse.equinox.p2.ui.RepositoryManipulator)
	 */
	protected int getRepositoryFlags(RepositoryManipulator repositoryManipulator) {
		return repositoryManipulator.getMetadataRepositoryFlags();
	}
}
