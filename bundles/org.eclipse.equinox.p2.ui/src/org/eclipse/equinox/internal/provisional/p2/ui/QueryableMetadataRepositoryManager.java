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
import java.util.Arrays;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * An object that adds provides specialized/optimized queryable support 
 * for a metadata repository.  The policy determines which repositories are included in 
 * the query.  Callers interested in only the resulting repository URIs
 * should specify a {@link RepositoryLocationQuery}, in which case the 
 * query is performed over the URI's.  Otherwise the query is performed over
 * the repositories themselves.
 */
public class QueryableMetadataRepositoryManager extends QueryableRepositoryManager {

	public QueryableMetadataRepositoryManager(Policy policy, boolean includeDisabledRepos) {
		super(policy, includeDisabledRepos);
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
			return ((IMetadataRepositoryManager) manager).loadRepository(location, monitor);
		}
		return null;
	}

	protected Collector query(URI uri, Query query, Collector collector, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, NLS.bind(ProvUIMessages.QueryableMetadataRepositoryManager_RepositoryQueryProgress, uri.toString()), 200);
		if (query instanceof RepositoryLocationQuery) {
			query.perform(Arrays.asList(new URI[] {uri}).iterator(), collector);
			sub.worked(2);
		} else {
			IRepository repo = null;
			try {
				repo = loadRepository(getRepositoryManager(), uri, sub.newChild(100));
			} catch (ProvisionException e) {
				if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
					handleNotFound(e, uri);
				else
					ProvUI.handleException(e, NLS.bind(ProvUIMessages.ProvisioningUtil_LoadRepositoryFailure, uri), StatusManager.LOG);
			} catch (OperationCanceledException e) {
				// user has canceled
				repo = null;
			}
			if (repo instanceof IMetadataRepository)
				((IMetadataRepository) repo).query(query, collector, sub.newChild(100));
		}
		monitor.done();
		return collector;
	}
}
