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
package org.eclipse.equinox.p2.artifact.repository;

import java.net.URL;
import java.util.Properties;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

public interface IArtifactRepositoryManager {
	public static final IArtifactRequest[] NO_ARTIFACT_REQUEST = new IArtifactRequest[0];

	/**
	 * Return the list of artifact repositories known by this manager.
	 * @return the list of known repositories
	 */
	public IArtifactRepository[] getKnownRepositories();

	/**
	 * Add a repository at the given location.  The location is expected to contain 
	 * data that describes a valid artifact repository of a known type.  If this manager
	 * already knows a repository at the given location then that repository is returned.
	 * @param location the location in which to look for a repository description
	 * @monitor 
	 * @return a repository object for the given location or <code>null</code> if a repository
	 * could not be found or loaded.
	 */
	public IArtifactRepository loadRepository(URL location, IProgressMonitor monitor);

	/**
	 * Add the given repository to the set of repositories managed by this manager.
	 * @param repository the repository to add
	 */
	public void addRepository(IArtifactRepository repository);

	/**
	 * Return the artifact repository at the given location if known by this manager.  
	 * Otherwise return <code>null</code>
	 * @param location the location of the repository to return
	 * @return the found repository
	 */
	public IArtifactRepository getRepository(URL location);

	/**
	 * Remove the given repository from this manager.  Do nothing if the repository
	 * is not currently managed.
	 * @param toRemove the repository to remove
	 */
	public void removeRepository(IArtifactRepository toRemove);

	/**
	 * Creates and returns an artifact repository of the given type at the given location.
	 * If a repository already exists at that location <code>null</code> is returned.
	 * @param location the location for the new repository
	 * @param name the name of the new repo
	 * @param type the kind of repository to create
	 * @return the discovered or created repository
	 */
	public IArtifactRepository createRepository(URL location, String name, String type);

	/**
	 * Return a new request to download the given artifact and store it at the given destination.
	 * @param key the artifact to download
	 * @param destination the destination where the artifact will be stored
	 * @return the newly created request object
	 */
	public IArtifactRequest createDownloadRequest(IArtifactKey key, IPath destination);

	/**
	 * Return a new request to mirror the given artifact into the destination repository.
	 * @param key the artifact to mirror
	 * @param destination the destination where the artifact will be mirrored
	 * @return the newly created request object
	 */
	public IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination);

	/**
	 * Return a new request to mirror the given artifact into the destination repository.
	 * @param key the artifact to mirror
	 * @param destination the destination where the artifact will be mirrored
	 * @param destinationDescriptorProperties additional properties for use in creating the repositor's ArtifactDescriptor
	 * @return the newly created request object
	 */
	public IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination, Properties destinationDescriptorProperties);

}
