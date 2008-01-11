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
package org.eclipse.equinox.spi.p2.metadata.repository;

import java.net.URL;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;

public interface IMetadataRepositoryFactory {

	/**
	 * Creates and returns a new empty metadata repository of the given type at 
	 * the given location.
	 * 
	 * @param location the location for the new repository
	 * @param name the name of the new repository
	 * @param type the kind of repository to create
	 * @return the newly created repository
	 * @throws ProvisionException if the repository could not be created.  Reasons include:
	 * <ul>
	 * <li>The repository type is not supported by this factory.</li>
	 * <li>There was an error writing to the given repository location.</li>
	 * </ul>
	 */
	public IMetadataRepository create(URL location, String name, String type) throws ProvisionException;

	/**
	 * Loads a repository corresponding to the given URL.
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
	 */
	public IMetadataRepository load(URL location, IProgressMonitor monitor) throws ProvisionException;
}
