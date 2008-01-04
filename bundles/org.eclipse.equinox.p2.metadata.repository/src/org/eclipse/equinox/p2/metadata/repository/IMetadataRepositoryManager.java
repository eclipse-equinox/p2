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
import org.eclipse.equinox.p2.query.IQueryable;

public interface IMetadataRepositoryManager extends IQueryable {
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
	 * @return the locations of the repositories managed by this repository manager.
	 */
	public URL[] getKnownRepositories();

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
}
