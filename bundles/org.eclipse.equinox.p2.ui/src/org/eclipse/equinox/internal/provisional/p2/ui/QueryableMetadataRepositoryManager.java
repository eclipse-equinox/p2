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
package org.eclipse.equinox.internal.provisional.p2.ui;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IUViewQueryContext;

/**
 * An object that adds provides specialized/optimized queryable support 
 * for a metadata repository.  The query context determines which repositories 
 * are included in the query.  Callers interested in only the resulting repository URIs
 * should specify a {@link RepositoryLocationQuery}, in which case the 
 * query is performed over the URI's.  Otherwise the query is performed over
 * the repositories themselves.
 */
public class QueryableMetadataRepositoryManager extends QueryableRepositoryManager {

	public QueryableMetadataRepositoryManager(IUViewQueryContext queryContext, boolean includeDisabledRepos) {
		super(queryContext, includeDisabledRepos);
	}

	protected IRepository getRepository(IRepositoryManager manager, URI location) {
		// note the use of MetadataRepositoryManager (the concrete implementation).
		if (manager instanceof MetadataRepositoryManager) {
			return ((MetadataRepositoryManager) manager).getRepository(location);
		}
		return super.getRepository(manager, location);
	}

	protected IRepositoryManager getRepositoryManager() {
		return (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
	}

	protected IRepository doLoadRepository(IRepositoryManager manager, URI location, IProgressMonitor monitor) throws ProvisionException {
		if (manager instanceof IMetadataRepositoryManager) {
			return ProvisioningUtil.loadMetadataRepository(location, monitor);
		}
		return null;
	}

	protected Collector query(URI uris[], Query query, Collector collector, IProgressMonitor monitor) {
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
		int flags = queryContext.getMetadataRepositoryFlags();
		locations.addAll(Arrays.asList(manager.getKnownRepositories(flags)));
		if (includeDisabledRepos) {
			locations.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED | flags)));
		}
		return (URI[]) locations.toArray(new URI[locations.size()]);
	}
}
