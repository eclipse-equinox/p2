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
package org.eclipse.equinox.p2.artifact.repository;

import java.net.URL;
import java.util.Properties;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

public interface IArtifactRepositoryManager {
	/**
	 * Constant used to indicate that all repositories are of interest.
	 */
	public static final int REPOSITORIES_ALL = 0;
	/**
	 * Constant used to indicate that implementation-only repositories are of interest.
	 */
	public static final int REPOSITORIES_IMPLEMENTATION_ONLY = 1 << 1;
	/**
	 * Constant used to indicate that public (non-implementation-only) repositories are of interest.
	 */
	public static final int REPOSITORIES_PUBLIC_ONLY = 1 << 2;
	/**
	 * Constant used to indicate that local repositories are of interest.
	 */
	public static final int REPOSITORIES_LOCAL_ONLY = 1 << 3;

	/**
	 * Property key used to query a repository's name without loading the repository first.
	 */
	public static final String PROP_NAME = "name"; //$NON-NLS-1$
	/**
	 * Property key used to query a repository's description without loading the repository first.
	 */
	public static final String PROP_DESCRIPTION = "description"; //$NON-NLS-1$

	public static final IArtifactRequest[] NO_ARTIFACT_REQUEST = new IArtifactRequest[0];

	/**
	 * Adds a repository to the list of artifact repositories tracked by the repository
	 * manager.
	 * 
	 * @param location The location of the artifact repository to add
	 */
	public void addRepository(URL location);

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
	 * @param destinationRepositoryProperties additional repository specific properties for use in creating the repositor's ArtifactDescriptor
	 * @return the newly created request object
	 */
	public IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination, Properties destinationDescriptorProperties, Properties destinationRepositoryProperties);

	/**
	 * Creates and returns an artifact repository of the given type at the given location.
	 * If a repository already exists at that location <code>null</code> is returned.
	 * @param location the location for the new repository
	 * @param name the name of the new repository
	 * @param type the kind of repository to create
	 * @return the discovered or created repository
	 */
	public IArtifactRepository createRepository(URL location, String name, String type);

	/**
	 * Returns the artifact repository locations known to the repository manager.
	 * <p>
	 * Note that the repository manager does not guarantee that a valid repository
	 * exists at any of the returned locations at any particular moment in time.
	 * A subsequent attempt to load a repository at any of the given locations may
	 * or may not succeed.
	 * 
	 * @param flags an integer bit-mask indicating which repositories should be
	 * returned.  <code>REPOSITORIES_ALL</code> can be used as the mask when
	 * all repositories should be returned.
	 * 
	 * @return the locations of the repositories managed by this repository manager.
	 * 
	 * @see #REPOSITORIES_ALL
	 * @see #REPOSITORIES_IMPLEMENTATION_ONLY
	 * @see #REPOSITORIES_LOCAL_ONLY
	 * @see #REPOSITORIES_PUBLIC_ONLY
	 */
	public URL[] getKnownRepositories(int flags);

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

	/**
	 * Loads the repository at the given location.  The location is expected to contain 
	 * data that describes a valid artifact repository of a known type.  If this manager
	 * already knows a repository at the given location then that repository is returned.
	 * 
	 * @param location the location in which to look for a repository description
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return a repository object for the given location or <code>null</code> if a repository
	 * could not be found or loaded.
	 */
	public IArtifactRepository loadRepository(URL location, IProgressMonitor monitor);

	/**
	 * Remove the given repository from this manager.  Do nothing if the repository
	 * is not currently managed.
	 * 
	 * @param location the location of the repository to remove
	 * @return <code>true</code> if a repository was removed, and 
	 * <code>false</code> otherwise.
	 */
	public boolean removeRepository(URL location);
}