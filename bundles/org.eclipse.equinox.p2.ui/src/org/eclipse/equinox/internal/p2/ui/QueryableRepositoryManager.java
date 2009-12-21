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
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.osgi.util.NLS;

/**
 * An object that provides query support for a specified
 * set of repositories.  The repository tracker flags determine which repositories
 * are included in the query.  Callers interested in only the resulting repository URIs
 * should specify a {@link RepositoryLocationQuery}, in which case the 
 * query is performed over the URI's.  Otherwise the repositories are loaded and
 * the query is performed over the repositories themselves.
 */
public abstract class QueryableRepositoryManager implements IQueryable {
	private ProvisioningSession session;
	protected boolean includeDisabledRepos;
	protected RepositoryTracker tracker;
	protected int repositoryFlags;
	protected ProvisioningUI ui;

	public QueryableRepositoryManager(ProvisioningUI ui, boolean includeDisabledRepos) {
		this.includeDisabledRepos = includeDisabledRepos;
		this.ui = ui;
		this.tracker = ui.getRepositoryTracker();
		this.session = ui.getSession();
		repositoryFlags = getRepositoryFlags(tracker);
	}

	protected ProvisioningSession getSession() {
		return session;
	}

	/**
	 * Iterates over the repositories configured in this queryable.
	 * For most queries, the query is run on each repository, passing any objects that satisfy the
	 * query.  If the query is a {@link RepositoryLocationQuery}, the query
	 * is run on the repository locations instead.
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor. 
	 * </p>
	 * 
	 * @param query The query to perform..
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The QueryResult argument
	 */
	public IQueryResult query(IQuery query, IProgressMonitor monitor) {
		IRepositoryManager manager = getRepositoryManager();
		if (monitor == null)
			monitor = new NullProgressMonitor();
		return query(getRepoLocations(manager), query, monitor);
	}

	protected URI[] getRepoLocations(IRepositoryManager manager) {
		Set locations = new HashSet();
		locations.addAll(Arrays.asList(manager.getKnownRepositories(repositoryFlags)));
		if (includeDisabledRepos) {
			locations.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED | repositoryFlags)));
		}
		return (URI[]) locations.toArray(new URI[locations.size()]);
	}

	/**
	 * Return a boolean indicating whether all the repositories that
	 * can be queried by the receiver are already loaded.  If a repository
	 * is not loaded because it was not found, this will not return false,
	 * because this repository cannot be queried.
	 * 
	 * @return <code>true</code> if all repositories to be queried by the
	 * receiver are loaded, <code>false</code> if they
	 * are not.
	 */
	public boolean areRepositoriesLoaded() {
		IRepositoryManager mgr = getRepositoryManager();
		if (mgr == null)
			return false;
		URI[] repoURIs = getRepoLocations(mgr);
		for (int i = 0; i < repoURIs.length; i++) {
			IRepository repo = getRepository(mgr, repoURIs[i]);
			// A not-loaded repo doesn't count if it's considered missing (not found)
			if (repo == null && !tracker.hasNotFoundStatusBeenReported(repoURIs[i]))
				return false;
		}
		return true;
	}

	protected abstract IRepository getRepository(IRepositoryManager manager, URI location);

	protected IRepository loadRepository(IRepositoryManager manager, URI location, IProgressMonitor monitor) throws ProvisionException {
		monitor.setTaskName(NLS.bind(ProvUIMessages.QueryableMetadataRepositoryManager_LoadRepositoryProgress, URIUtil.toUnencodedString(location)));
		IRepository repo = doLoadRepository(manager, location, monitor);
		return repo;
	}

	/**
	 * Return the appropriate repository manager, or <code>null</code> if none could be found.
	 * @return the repository manager
	 */
	protected abstract IRepositoryManager getRepositoryManager();

	/**
	 * Return the flags that should be used to access repositories given the
	 * manipulator.
	 */
	protected abstract int getRepositoryFlags(RepositoryTracker repositoryManipulator);

	/**
	 * Load the repository located at the specified location.
	 * 
	 * @param manager the manager
	 * @param location the repository location
	 * @param monitor the progress monitor
	 * @return the repository that was loaded, or <code>null</code> if no repository could
	 * be found at that location.
	 */
	protected abstract IRepository doLoadRepository(IRepositoryManager manager, URI location, IProgressMonitor monitor) throws ProvisionException;

	protected IQueryResult query(URI uris[], IQuery query, IProgressMonitor monitor) {
		if (query instanceof RepositoryLocationQuery) {
			return query.perform(Arrays.asList(uris).iterator());
		}

		SubMonitor sub = SubMonitor.convert(monitor, (uris.length + 1) * 100);
		ArrayList loadedRepos = new ArrayList(uris.length);
		for (int i = 0; i < uris.length; i++) {
			IRepository repo = null;
			try {
				repo = loadRepository(getRepositoryManager(), uris[i], sub.newChild(100));
			} catch (ProvisionException e) {
				tracker.reportLoadFailure(uris[i], e);
			} catch (OperationCanceledException e) {
				// user has canceled
				repo = null;
			}
			if (repo != null)
				loadedRepos.add(repo);
		}
		if (loadedRepos.size() > 0) {
			IQueryable[] queryables = (IQueryable[]) loadedRepos.toArray(new IQueryable[loadedRepos.size()]);
			return new CompoundQueryable(queryables).query(query, sub.newChild(100));
		}
		return Collector.EMPTY_COLLECTOR;
	}

	public void setRespositoryFlags(int flags) {
		this.repositoryFlags = flags;
	}

}
