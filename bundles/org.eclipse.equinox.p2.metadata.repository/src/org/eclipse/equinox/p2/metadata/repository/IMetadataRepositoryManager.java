/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
	 * Adds a new metadata repository to the set of known repositories.
	 * @param repository
	 */
	public void addRepository(IMetadataRepository repository);

	/**
	 * Creates and returns a metadata repository of the given type at the given location.
	 * If a repository already exists at that location <code>null</code> is returned.
	 * @param location the location for the new repository
	 * @param name the name of the new repository
	 * @param type the kind of repository to create
	 * @return the discovered or created repository
	 */
	public IMetadataRepository createRepository(URL location, String name, String type);

	/**
	 * Returns the locations of the repositories managed by this repository manager.
	 * 
	 * @return the locations of the repositories managed by this repository manager.
	 */
	public URL[] getKnownRepositories();

	/**
	 * Adds a repository corresponding to the given URL.
	 * 
	 * @param url The URL of the repository to add
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 */
	public IMetadataRepository loadRepository(URL url, IProgressMonitor monitor);

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
