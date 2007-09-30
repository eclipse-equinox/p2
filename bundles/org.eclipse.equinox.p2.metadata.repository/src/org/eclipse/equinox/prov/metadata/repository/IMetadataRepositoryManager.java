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
package org.eclipse.equinox.prov.metadata.repository;

import java.net.URL;
import org.eclipse.core.runtime.IProgressMonitor;

public interface IMetadataRepositoryManager {
	/**
	 * Adds a new metadata repository to the set of known repositories.
	 * @param repository
	 */
	public void addRepository(IMetadataRepository repository);

	/**
	 * Adds a repository corresponding to the given URL.
	 * @param url The URL of the repository to add
	 * @param progress TODO
	 */
	public IMetadataRepository loadRepository(URL url, IProgressMonitor progress);

	/**
	 * Creates and returns a metadata repository of the given type at the given location.
	 * If a repository already exists at that location <code>null</code> is returned.
	 * @param location the location for the new repository
	 * @param name the name of the new repo
	 * @param type the kind of repository to create
	 * @return the discovered or created repository
	 */
	public IMetadataRepository createRepository(URL location, String name, String type);

	public IMetadataRepository[] getKnownRepositories();

	public IMetadataRepository getRepository(URL repo); //TODO Should this throw an exception

	public void removeRepository(IMetadataRepository toRemove);
}
