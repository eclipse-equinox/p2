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
package org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;

/**
 * An artifact repository factory is responsible for creating and loading instances
 * of a particular type of artifact repository. Factories are provided via the 
 * <tt>org.eclipse.equinox.p2.artifact.repository.artifactRepositories</tt> extension point.
 */
public abstract class ArtifactRepositoryFactory {

	/**
	 * Creates and returns a new empty artifact repository of the given type at 
	 * the given location.
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
	public abstract IArtifactRepository create(URI location, String name, String type, Map properties) throws ProvisionException;

	/**
	 * Loads and returns the repository of this factory's type at the given location. 
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
	 * fast and return null.
	 * @see IRepositoryManager#REPOSITORY_HINT_MODIFIABLE
	 * </p>
	 * @param location the location in which to look for a repository description
	 * @param flags to consider while loading the repository
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return a repository object for the given location
	 * @throws ProvisionException if the repository could not be created.  Reasons include:
	 * <ul>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 */
	public abstract IArtifactRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException;
}
