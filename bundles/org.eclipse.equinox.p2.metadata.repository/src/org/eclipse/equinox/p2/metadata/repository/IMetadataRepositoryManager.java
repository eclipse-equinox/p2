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
package org.eclipse.equinox.p2.metadata.repository;

import java.net.URL;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.query.IQueryable;

public interface IMetadataRepositoryManager extends IQueryable {

	/**
	 * Constant used to indicate that all repositories are of interest.
	 */
	public static final int REPOSITORIES_ALL = 0;
	/**
	 * Constant used to indicate that system repositories are of interest.
	 * @see IRepository#PROP_SYSTEM
	 */
	public static final int REPOSITORIES_SYSTEM = 1 << 1;
	/**
	 * Constant used to indicate that non-system repositories are of interest
	 * @see IRepository#PROP_SYSTEM
	 */
	public static final int REPOSITORIES_NON_SYSTEM = 1 << 2;
	/**
	 * Constant used to indicate that local repositories are of interest.
	 */
	public static final int REPOSITORIES_LOCAL = 1 << 3;

	/**
	 * Property key used to query a repository's name without loading the repository first.
	 */
	public static final String PROP_NAME = "name"; //$NON-NLS-1$
	/**
	 * Property key used to query a repository's description without loading the repository first.
	 */
	public static final String PROP_DESCRIPTION = "description"; //$NON-NLS-1$

	/**
	 * Repository type for a simple repository based on a URL or local file system location.
	 */
	public static final String TYPE_SIMPLE_REPOSITORY = "org.eclipse.equinox.p2.metadata.repository.simpleRepository"; //$NON-NLS-1$

	/**
	 * Adds a repository to the list of metadata repositories tracked by the repository
	 * manager.
	 * @param location The location of the metadata repository to add
	 */
	public void addRepository(URL location);

	/**
	 * Creates and returns a metadata repository of the given type at the given location.
	 * If a repository already exists at that location <code>null</code> is returned.
	 * 
	 * @param location the location for the new repository
	 * @param name the name of the new repository
	 * @param type the kind of repository to create
	 * @return the discovered or created repository
	 */
	public IMetadataRepository createRepository(URL location, String name, String type);

	/**
	 * Returns the metadata repository locations known to the repository manager.
	 * <p>
	 * Note that the repository manager does not guarantee that a valid repository
	 * exists at any of the returned locations at any particular moment in time.
	 * A subsequent attempt to load a repository at any of the given locations may
	 * or may not succeed.
	 * 
	 * @param flags an integer bit-mask indicating which repositories should be
	 * returned.  <code>REPOSITORIES_ALL</code> can be used as the mask when
	 * all repositories should be returned.  Where multiple masks are combined, only
	 * the repositories that satisfy all the given criteria are returned. For example,
	 * a flag value of )REPOSITORIES_SYSTEM|REPOSITORIES_LOCAL) will only
	 * return repositories that are both system and local repositories.
	 * 
	 * @return the locations of the repositories managed by this repository manager.
	 * 
	 * @see #REPOSITORIES_ALL
	 * @see #REPOSITORIES_SYSTEM
	 * @see #REPOSITORIES_LOCAL
	 * @see #REPOSITORIES_NON_SYSTEM
	 */
	public URL[] getKnownRepositories(int flags);

	/**
	 * Loads a repository corresponding to the given URL.
	 * 
	 * @param location The location of the repository to load
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 */
	public IMetadataRepository loadRepository(URL location, IProgressMonitor monitor);

	/**
	 * Removes the metadata repository at the given location from the list of
	 * metadata repositories tracked by the repository manager.  The underlying
	 * repository is not deleted.
	 * 
	 * @param location The location of the repository to remove
	 * @return <code>true</code> if a repository was removed, and 
	 * <code>false</code> otherwise.
	 */
	public boolean removeRepository(URL location);

	/**
	 * Returns the property associated with the repository at the given URL, 
	 * without loading the repository.
	 * <p>
	 * Note that some properties for a repository can only be
	 * determined when that repository is loaded.  This method will return <code>null</code>
	 * for such properties.  Only values for the properties that are already
	 * known by a repository manager will be returned. 
	 * <p>
	 * If a client wishes to retrieve a property value from a repository 
	 * regardless of the cost of retrieving it, the client should load the 
	 * repository and then retrieve the property from the repository itself.
	 * 
	 * @param location the URL of the repository in question
	 * @param key the String key of the property desired
	 * @return the value of the property, or <code>null</code> if the repository
	 * does not exist, the value does not exist, or the property value 
	 * could not be determined without loading the repository.
	 * 
	 * @see #loadRepository(URL, IProgressMonitor)
	 * @see IRepository#getProperties()
	 * 
	 */
	public String getRepositoryProperty(URL location, String key);
}
