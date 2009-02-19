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
package org.eclipse.equinox.internal.provisional.p2.metadata.repository;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;

/**
 * A metadata repository manager is used to create, access, and manipulate
 * {@link IMetadataRepository} instances. See {@link IRepositoryManager}
 * for a general description of the characteristics of repository managers.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMetadataRepositoryManager extends IRepositoryManager, IQueryable {
	/**
	 * The name used for obtaining a reference to the metadata repository manager service
	 */
	public static final String SERVICE_NAME = IMetadataRepositoryManager.class.getName();

	/**
	 * Repository type for a simple repository based on a URL or local file system location.
	 */
	public static final String TYPE_SIMPLE_REPOSITORY = "org.eclipse.equinox.p2.metadata.repository.simpleRepository"; //$NON-NLS-1$
	public static final String TYPE_COMPOSITE_REPOSITORY = "org.eclipse.equinox.p2.metadata.repository.compositeRepository"; //$NON-NLS-1$

	/**
	 * Creates and returns a new empty metadata repository of the given type at 
	 * the given location.
	 * <p>
	 * The resulting repository is added to the list of repositories tracked by
	 * the repository manager. Clients must make a subsequent call to {@link #removeRepository(URI)}
	 * if they do not want the repository manager to remember the repository for subsequent
	 * load attempts.
	 * </p>
	 * 
	 * @param location the location for the new repository
	 * @param name the name of the new repository
	 * @param type the kind of repository to create
	 * @param properties the properties to set on the repository
	 * @return the newly created repository
	 * @throws ProvisionException if the repository could not be created.  Reasons include:
	 * <ul>
	 * <li>The repository type is unknown.</li>
	 * <li>There was an error writing to the given repository location.</li>
	 * <li>A repository already exists at that location.</li>
	 * </ul>
	 */
	public IMetadataRepository createRepository(URI location, String name, String type, Map properties) throws ProvisionException;

	/**
	 * Loads a repository corresponding to the given URL.  If a repository has
	 * previously been loaded at the given location, the same cached repository
	 * may be returned.
	 * <p>
	 * The resulting repository is added to the list of repositories tracked by
	 * the repository manager. Clients must make a subsequent call to {@link #removeRepository(URI)}
	 * if they do not want the repository manager to remember the repository for subsequent
	 * load attempts.
	 * </p>
	 * 
	 * @param location The location of the repository to load
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The loaded metadata repository
	 * @throws ProvisionException if the repository could not be created.  Reasons include:
	 * <ul>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 * @deprecated see {@link #loadRepository(URI, int, IProgressMonitor)}
	 */
	public IMetadataRepository loadRepository(URI location, IProgressMonitor monitor) throws ProvisionException;

	/**
	 * Loads a repository corresponding to the given URL.  If a repository has
	 * previously been loaded at the given location, the same cached repository
	 * may be returned.
	 * <p>
	 * The resulting repository is added to the list of repositories tracked by
	 * the repository manager. Clients must make a subsequent call to {@link #removeRepository(URI)}
	 * if they do not want the repository manager to remember the repository for subsequent
	 * load attempts.
	 * </p>
	 * <p>
	 * The flags passed in should be taken as a hint for the type of repository to load.  If
	 * the manager will not load a repository that satisfies these hints, it can fail
	 * fast.<br>
	 * See {@link IRepositoryManager#REPOSITORY_HINT_MODIFIABLE}
	 * </p>
	 * @param location The location of the repository to load
	 * @param flags - flags to consider when loading the repository
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The loaded metadata repository
	 * @throws ProvisionException if the repository could not be created.  Reasons include:
	 * <ul>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 */
	public IMetadataRepository loadRepository(URI location, int flags, IProgressMonitor monitor) throws ProvisionException;

	/**
	 * Refreshes the repository corresponding to the given URL. This method discards
	 * any cached state held by the repository manager and reloads the repository
	 * contents. The provided repository location must already be known to the repository
	 * manager.
	 * 
	 * @param location The location of the repository to refresh
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The refreshed metadata repository
	 * @throws ProvisionException if the repository could not be refreshed.  Reasons include:
	 * <ul>
	 * <li>The location is not known to the repository manager.</li>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 */
	public IMetadataRepository refreshRepository(URI location, IProgressMonitor monitor) throws ProvisionException;

	/**
	 * Validates a given URL and returns a status indicating whether a valid repository is likely
	 * to be found at the given URL.  Callers must assume that the validity of a 
	 * repository location cannot be completely determined until an attempt to load 
	 * the repository is made.  
	 * 
	 * @param location The location of the repository to validate
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return A status indicating whether a valid repository is likely located at the
	 * location.  A status with severity <code>OK</code> indicates that the repository is
	 * likely to be loadable, or that as much validation as could be done was successful.
	 * Reasons for a non-OK status include:
	 * <ul>
	 * <li>The specified location is not a valid repository location.</li>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 */
	public IStatus validateRepositoryLocation(URI location, IProgressMonitor monitor);
}
