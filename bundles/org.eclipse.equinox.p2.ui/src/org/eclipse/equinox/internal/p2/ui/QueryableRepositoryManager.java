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
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * An object that provides specific/optimized query support for a specified
 * set of repositories.  The repository tracker flags determine which repositories
 * are included in the query.  Callers interested in only the resulting repository URIs
 * should specify a {@link RepositoryLocationQuery}, in which case the 
 * query is performed over the URI's.  Otherwise the repositories are loaded and
 * the query is performed over the repositories themselves.
 */
public abstract class QueryableRepositoryManager implements IQueryable {
	/**
	 * List<URI> of locations of repositories that were not found
	 */
	private ArrayList notFound = new ArrayList();

	/**
	 * Map<URI,IRepository> of loaded repositories.
	 */
	private HashMap loaded = new HashMap();

	private MultiStatus accumulatedNotFound = null;
	private ProvisioningSession session;
	protected boolean includeDisabledRepos;
	protected RepositoryTracker tracker;
	protected int repositoryFlags;

	public QueryableRepositoryManager(ProvisioningUI ui, boolean includeDisabledRepos) {
		this.includeDisabledRepos = includeDisabledRepos;
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
	 * query to the provided collector.  If the query is a {@link RepositoryLocationQuery}, the query
	 * is run on the repository locations instead.
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor. 
	 * </p>
	 * 
	 * @param query The query to perform..
	 * @param result Collects the results of the query, run on either the repository URIs, or on
	 *    the repositories themselves.
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The collector argument
	 */
	public Collector query(IQuery query, Collector result, IProgressMonitor monitor) {
		IRepositoryManager manager = getRepositoryManager();
		if (monitor == null)
			monitor = new NullProgressMonitor();
		query(getRepoLocations(manager), query, result, monitor);
		reportAccumulatedStatus();
		return result;
	}

	/**
	 * Load all of the repositories referenced by this queryable.  This is an expensive operation.
	 * The status of any not found repositories is accumulated and must be reported manually
	 * using reportAccumulatedStatus()
	 * 
	 * @param monitor the progress monitor that should be used
	 */
	public void loadAll(IProgressMonitor monitor) {
		IRepositoryManager manager = getRepositoryManager();
		URI[] repoLocations = getRepoLocations(manager);
		SubMonitor sub = SubMonitor.convert(monitor, repoLocations.length * 100);
		if (sub.isCanceled())
			return;
		for (int i = 0; i < repoLocations.length; i++) {
			if (sub.isCanceled())
				return;
			try {
				loadRepository(manager, repoLocations[i], sub.newChild(100));
			} catch (ProvisionException e) {
				handleLoadFailure(e, repoLocations[i]);
			}
		}
	}

	protected URI[] getRepoLocations(IRepositoryManager manager) {
		Set locations = new HashSet();
		locations.addAll(Arrays.asList(manager.getKnownRepositories(repositoryFlags)));
		if (includeDisabledRepos) {
			locations.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED | repositoryFlags)));
		}
		return (URI[]) locations.toArray(new URI[locations.size()]);
	}

	protected void handleLoadFailure(ProvisionException e, URI problemRepo) {
		int code = e.getStatus().getCode();
		// special handling when the repo is bad.  We don't want to continually report it
		if (code == ProvisionException.REPOSITORY_NOT_FOUND || code == ProvisionException.REPOSITORY_INVALID_LOCATION) {
			// If we thought we had loaded it, get rid of the reference
			loaded.remove(problemRepo);
			// If we've already reported a URL is not found, don't report again.
			if (notFound.contains(problemRepo))
				return;
			// If someone else reported a URL is not found, don't report again.
			if (tracker.hasNotFoundStatusBeenReported(problemRepo)) {
				notFound.add(problemRepo);
				return;
			}
			notFound.add(problemRepo);
			tracker.addNotFound(problemRepo);
		}

		// Some ProvisionExceptions include an empty multi status with a message.  
		// Since empty multi statuses have a severity OK, The platform status handler doesn't handle
		// this well.  We correct this by recreating a status with error severity
		// so that the platform status handler does the right thing.
		IStatus status = e.getStatus();
		if (status instanceof MultiStatus && ((MultiStatus) status).getChildren().length == 0)
			status = new Status(IStatus.ERROR, status.getPlugin(), status.getCode(), status.getMessage(), status.getException());
		if (accumulatedNotFound == null) {
			accumulatedNotFound = new MultiStatus(ProvUIActivator.PLUGIN_ID, ProvisionException.REPOSITORY_NOT_FOUND, new IStatus[] {status}, ProvUIMessages.QueryableMetadataRepositoryManager_MultipleRepositoriesNotFound, null);
		} else {
			accumulatedNotFound.add(status);
		}
		// Always log the complete exception so the detailed stack trace is in the log.  
		ProvUI.handleException(e, NLS.bind(ProvUIMessages.QueryableRepositoryManager_LoadFailure, problemRepo), StatusManager.LOG);

	}

	public void reportAccumulatedStatus() {
		// If we've discovered not found repos we didn't know about, report them
		if (accumulatedNotFound != null) {
			// If there is only missing repo to report, use the specific message rather than the generic.
			if (accumulatedNotFound.getChildren().length == 1) {
				ProvUI.reportStatus(accumulatedNotFound.getChildren()[0], StatusManager.SHOW);
			} else {
				ProvUI.reportStatus(accumulatedNotFound, StatusManager.SHOW);
			}
		}
		// Reset the accumulated status so that next time we only report the newly not found repos.
		accumulatedNotFound = null;
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

	protected IRepository loadRepository(IRepositoryManager manager, URI location, IProgressMonitor monitor) throws ProvisionException {
		monitor.setTaskName(NLS.bind(ProvUIMessages.QueryableMetadataRepositoryManager_LoadRepositoryProgress, URIUtil.toUnencodedString(location)));
		IRepository repo = doLoadRepository(manager, location, monitor);
		if (repo != null)
			loaded.put(location, repo);
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
	 * Get an already-loaded repository at the specified location.
	 * 
	 * @param manager the manager
	 * @param location the repository location
	 * @return the repository at that location, or <code>null</code> if no repository is
	 * yet located at that location.
	 */
	protected IRepository getRepository(IRepositoryManager manager, URI location) {
		// This is only used by the artifact mgr subclass.
		// MetadataRepositoryManager has a method for getting its cached repo instance
		return (IRepository) loaded.get(location);
	}

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

	public void setRespositoryFlags(int flags) {
		this.repositoryFlags = flags;
	}

}
