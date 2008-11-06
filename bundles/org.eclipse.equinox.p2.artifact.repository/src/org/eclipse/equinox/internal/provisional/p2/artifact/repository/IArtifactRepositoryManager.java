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
package org.eclipse.equinox.internal.provisional.p2.artifact.repository;

import java.net.URI;
import java.util.Map;
import java.util.Properties;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;

/**
 * A metadata repository manager is used to create, access, and manipulate
 * {@link IArtifactRepository} instances. See {@link IRepositoryManager}
 * for a general description of the characteristics of repository managers.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IArtifactRepositoryManager extends IRepositoryManager {
	/**
	 * The name used for obtaining a reference to the metadata repository manager service
	 */
	public static final String SERVICE_NAME = IArtifactRepositoryManager.class.getName();

	public static final IArtifactRequest[] NO_ARTIFACT_REQUEST = new IArtifactRequest[0];

	/**
	 * Repository type for a simple repository based on a URL or local file system location.
	 */
	public static final String TYPE_SIMPLE_REPOSITORY = "org.eclipse.equinox.p2.artifact.repository.simpleRepository"; //$NON-NLS-1$

	/**
	 * Return a new request to mirror the given artifact into the destination repository.
	 * @param key the artifact to mirror
	 * @param destination the destination where the artifact will be mirrored
	 * @param destinationDescriptorProperties additional properties for use in creating the repository's ArtifactDescriptor, 
	 * or <code>null</code> to indicate no additional properties are needed
	 * @param destinationRepositoryProperties additional repository specific properties for use in creating the repositor's ArtifactDescriptor,
	 * , or <code>null</code> to indicate no additional properties are needed
	 * @return the newly created request object
	 */
	public IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination, Properties destinationDescriptorProperties, Properties destinationRepositoryProperties);

	/**
	 * Creates and returns a new empty artifact repository of the given type at 
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
	public IArtifactRepository createRepository(URI location, String name, String type, Map properties) throws ProvisionException;

	/**
	 * Loads the repository at the given location.  The location is expected to contain 
	 * data that describes a valid artifact repository of a known type.  If this manager
	 * already knows a repository at the given location then that repository is returned.
	 * <p>
	 * The resulting repository is added to the list of repositories tracked by
	 * the repository manager. Clients must make a subsequent call to {@link #removeRepository(URI)}
	 * if they do not want the repository manager to remember the repository for subsequent
	 * load attempts.
	 * </p>
	 * 
	 * @param location the location in which to look for a repository description
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return a repository object for the given location
	 * @throws ProvisionException if the repository could not be created.  Reasons include:
	 * <ul>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 */
	public IArtifactRepository loadRepository(URI location, IProgressMonitor monitor) throws ProvisionException;

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
	 * @throws ProvisionException if the repository could not be created.  Reasons include:
	 * <ul>
	 * <li>The location is not known to the repository manager.</li>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 */
	public IArtifactRepository refreshRepository(URI location, IProgressMonitor monitor) throws ProvisionException;

}