/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository;

import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;

/**
 * A metadata repository factory is responsible for creating and loading instances
 * of a particular type of metadata repository. Factories are provided via the 
 * <tt>org.eclipse.equinox.p2.metadata.repository.metadataRepositories</tt> extension point.
 */
public abstract class MetadataRepositoryFactory {

	/**
	 * Creates and returns a new empty metadata repository of the given type at 
	 * the given location.
	 * 
	 * @param location the location for the new repository
	 * @param name the name of the new repository
	 * @param type the kind of repository to create
	 * @param properties the properties to set on the repository
	 * @return the newly created repository
	 * @throws ProvisionException if the repository could not be created.  Reasons include:
	 * <ul>
	 * <li>The repository type is not supported by this factory.</li>
	 * <li>There was an error writing to the given repository location.</li>
	 * </ul>
	 */
	public abstract IMetadataRepository create(URI location, String name, String type, Map properties) throws ProvisionException;

	/**
	 * Loads a repository corresponding to the given URL.
	 * <p>
	 * The error code returned in the case of failure is significant. In particular an
	 * error code of {@link ProvisionException#REPOSITORY_FAILED_READ} indicates
	 * that the location definitely identifies a repository of this type, but an error occurred
	 * while loading the repository. The repository manager will not attempt to load
	 * a repository from that location using any other factory.  An error code of
	 * {@link ProvisionException#REPOSITORY_NOT_FOUND} indicates there is no
	 * repository of this type at the given location, and the repository manager is free
	 * to try again with a different repository factory.
	 * </p>
	 * <p>
	 * The flags passed in should be taken as a hint for the type of repository to load.  If
	 * the factory knows it will not load a repository that satisfies these hints, it can fail
	 * fast and return null.<br>
	 * See {@link IRepositoryManager#REPOSITORY_HINT_MODIFIABLE}
	 * </p>
	 * @param location The location of the repository to load
	 * @param flags to consider while loading the repository
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The loaded metadata repository
	 * @throws ProvisionException if the repository could not be created.  Reasons include:
	 * <ul>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 */
	public abstract IMetadataRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException;

	/**
	 * Validates a candidate repository URL and returns a status indicating the
	 * likelihood of a valid repository being located at the location.  Implementors 
	 * should make all attempts to validate the URL that can be made without 
	 * actually loading the repository.  The computation for this method must be 
	 * significantly faster than loading the repository.  Early detectable error 
	 * conditions, such as the non-existence of the location, or an inability to read 
	 * the location, should be determined in this method.
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
	public abstract IStatus validate(URI location, IProgressMonitor monitor);
}
