/*******************************************************************************
 * Copyright (c) 2007, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Wind River - continuing development
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.artifact;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.IRepositoryManager;

/**
 * A metadata repository manager is used to create, access, and manipulate
 * {@link IArtifactRepository} instances. See {@link IRepositoryManager} for a
 * general description of the characteristics of repository managers.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IArtifactRepositoryManager extends IRepositoryManager<IArtifactKey> {
	/**
	 * The name used for obtaining a reference to the metadata repository manager service
	 */
	String SERVICE_NAME = IArtifactRepositoryManager.class.getName();

	/**
	 * A convenience constant representing an empty set of artifact requests.
	 */
	IArtifactRequest[] NO_ARTIFACT_REQUEST = new IArtifactRequest[0];

	/**
	 * Repository type for a simple repository based on a URL or local file system location.
	 */
	String TYPE_SIMPLE_REPOSITORY = "org.eclipse.equinox.p2.artifact.repository.simpleRepository"; //$NON-NLS-1$

	/**
	 * Repository type for a composite repository based on a URL or local file system location.
	 */
	String TYPE_COMPOSITE_REPOSITORY = "org.eclipse.equinox.p2.artifact.repository.compositeRepository"; //$NON-NLS-1$

	/**
	 * Return a new request to mirror the given artifact into the destination repository.
	 *
	 * @param key the artifact to mirror
	 * @param destination the destination where the artifact will be mirrored
	 * @param destinationDescriptorProperties additional properties for use in creating the repository's ArtifactDescriptor,
	 * or <code>null</code> to indicate no additional properties are needed
	 * @param destinationRepositoryProperties additional repository specific properties for use in creating the repositor's ArtifactDescriptor,
	 * or <code>null</code> to indicate no additional properties are needed
	 * @return the newly created request object
	 */
	IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination,
			Map<String, String> destinationDescriptorProperties, Map<String, String> destinationRepositoryProperties);

	/**
	 * Return a new request to mirror the given artifact into the destination repository.
	 *
	 * @param key the artifact to mirror
	 * @param destination the destination where the artifact will be mirrored
	 * @param destinationDescriptorProperties additional properties for use in creating the repository's ArtifactDescriptor,
	 * or <code>null</code> to indicate no additional properties are needed
	 * @param destinationRepositoryProperties additional repository specific properties for use in creating the repositor's ArtifactDescriptor,
	 * or <code>null</code> to indicate no additional properties are needed
	 * @param downloadStatsParameters additional customizable parameters for downloading statistics,
	 * or <code>null</code> to indicate no additional customizable stats parameters
	 * @return the newly created request object
	 * @see IArtifactRepositoryManager#createMirrorRequest(IArtifactKey, IArtifactRepository, Map, Map)
	 * @since 2.2
	 */
	IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination,
			Map<String, String> destinationDescriptorProperties, Map<String, String> destinationRepositoryProperties,
			String downloadStatsParameters);

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
	 * @param location the absolute location for the new repository
	 * @param name the name of the new repository
	 * @param type the kind of repository to create
	 * @param properties the properties to set on the repository
	 * @return the newly created repository
	 * @throws ProvisionException if the repository could not be created. Reasons include:
	 * <ul>
	 * <li>The repository type is unknown.</li>
	 * <li>There was an error writing to the given repository location.</li>
	 * <li>A repository already exists at that location.</li>
	 * </ul>
	 */
	@Override
	IArtifactRepository createRepository(URI location, String name, String type, Map<String, String> properties)
			throws ProvisionException;

	/**
	 * Loads the repository at the given location. The location is expected to contain
	 * data that describes a valid artifact repository of a known type. If a repository has
	 * previously been loaded at that location, the same cached repository may be returned.
	 * <p>
	 * The resulting repository is added to the list of repositories tracked by
	 * the repository manager. Clients must make a subsequent call to {@link #removeRepository(URI)}
	 * if they do not want the repository manager to remember the repository for subsequent
	 * load attempts.
	 * </p>
	 *
	 * @param location the absolute location of the repository to load
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return the loaded artifact repository
	 * @throws ProvisionException if the repository could not be created. Reasons include:
	 * <ul>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 */
	@Override
	IArtifactRepository loadRepository(URI location, IProgressMonitor monitor) throws ProvisionException;

	/**
	 * Loads the repository at the given location. The location is expected to contain
	 * data that describes a valid artifact repository of a known type. If a repository
	 * has previously been loaded at that location, the same cached repository may be returned.
	 * <p>
	 * The resulting repository is added to the list of repositories tracked by
	 * the repository manager. Clients must make a subsequent call to {@link #removeRepository(URI)}
	 * if they do not want the repository manager to remember the repository for subsequent
	 * load attempts.
	 * </p>
	 * <p>
	 * The flags passed in should be taken as a hint for the type of repository to load. If
	 * the manager cannot load a repository that satisfies these hints, it can fail fast.
	 * </p>
	 *
	 * @param location the absolute location of the repository to load
	 * @param flags bit-wise or of flags to consider when loading the repository
	 *  (currently only {@link IRepositoryManager#REPOSITORY_HINT_MODIFIABLE} is supported)
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return the loaded artifact repository
	 * @throws ProvisionException if the repository could not be created. Reasons include:
	 * <ul>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 * @see IRepositoryManager#REPOSITORY_HINT_MODIFIABLE
	 */
	@Override
	IArtifactRepository loadRepository(URI location, int flags, IProgressMonitor monitor) throws ProvisionException;

	/**
	 * Refreshes the repository corresponding to the given URL. This method discards
	 * any cached state held by the repository manager and reloads the repository
	 * contents. The provided repository location must already be known to the repository
	 * manager.
	 *
	 * @param location The absolute location of the repository to refresh
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The refreshed metadata repository
	 * @throws ProvisionException if the repository could not be refreshed. Reasons include:
	 * <ul>
	 * <li>The location is not known to the repository manager.</li>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 */
	@Override
	IArtifactRepository refreshRepository(URI location, IProgressMonitor monitor) throws ProvisionException;

}
