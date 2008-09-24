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

import java.net.URL;
import java.util.Map;
import java.util.Properties;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryEvent;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;

/**
 * The metadata repository manager is used to create, access, and manipulate
 * {@link IArtifactRepository} instances. The manager keeps track of a 
 * set of known repositories, and provides caching of these known repositories
 * to avoid unnecessary loading of repositories from the disk or network.  The
 * manager fires {@link RepositoryEvent}s when the set of known repositories
 * changes.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IArtifactRepositoryManager {
	public static final IArtifactRequest[] NO_ARTIFACT_REQUEST = new IArtifactRequest[0];

	/**
	 * Constant used to indicate that all repositories are of interest.
	 * @see #getKnownRepositories(int)
	 */
	public static final int REPOSITORIES_ALL = 0;

	/**
	 * Constant used to indicate that system repositories are of interest.
	 * @see IRepository#PROP_SYSTEM
	 * @see #getKnownRepositories(int)
	 */
	public static final int REPOSITORIES_SYSTEM = 1 << 0;

	/**
	 * Constant used to indicate that non-system repositories are of interest
	 * @see IRepository#PROP_SYSTEM
	 * @see #getKnownRepositories(int)
	 */
	public static final int REPOSITORIES_NON_SYSTEM = 1 << 1;

	/**
	 * Constant used to indicate that local repositories are of interest.
	 * @see #getKnownRepositories(int)
	 */
	public static final int REPOSITORIES_LOCAL = 1 << 2;

	/**
	 * Constant used to indicate that disabled repositories are of interest.
	 * @see #getKnownRepositories(int)
	 */
	public static final int REPOSITORIES_DISABLED = 1 << 3;

	/**
	 * Repository type for a simple repository based on a URL or local file system location.
	 */
	public static final String TYPE_SIMPLE_REPOSITORY = "org.eclipse.equinox.p2.artifact.repository.simpleRepository"; //$NON-NLS-1$

	/**
	 * Adds a repository to the list of artifact repositories tracked by the repository
	 * manager.
	 * 
	 * @param location The location of the artifact repository to add
	 */
	public void addRepository(URL location);

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
	 * the repository manager. Clients must make a subsequent call to {@link #removeRepository(URL)}
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
	public IArtifactRepository createRepository(URL location, String name, String type, Map properties) throws ProvisionException;

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
	 * all enabled repositories should be returned.
	 * 
	 * @return the locations of the repositories managed by this repository manager.
	 * 
	 * @see #REPOSITORIES_ALL
	 * @see #REPOSITORIES_SYSTEM
	 * @see #REPOSITORIES_NON_SYSTEM
	 * @see #REPOSITORIES_LOCAL
	 * @see #REPOSITORIES_DISABLED
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
	 * Returns the enablement value of a repository.  Disabled repositories are known
	 * to the repository manager, but are never used in the context of provisioning
	 * operation. Disabled repositories are useful as a form of bookmark to indicate that a 
	 * repository location is of interest, but not currently used.
	 * <p>
	 * Note that enablement is a property of the repository manager and not a property
	 * of the affected repository. The enablement of the repository is discarded when 
	 * a repository is removed from the repository manager.
	 * 
	 * @param location The location of the repository whose enablement is requested
	 * @return <code>true</code> if the repository is enabled, and
	 * <code>false</code> if it is not enabled, or if the repository location 
	 * is not known to the repository manager.
	 * @see #REPOSITORIES_DISABLED
	 * @see #setEnabled(URL, boolean)
	 */
	public boolean isEnabled(URL location);

	/**
	 * Loads the repository at the given location.  The location is expected to contain 
	 * data that describes a valid artifact repository of a known type.  If this manager
	 * already knows a repository at the given location then that repository is returned.
	 * <p>
	 * The resulting repository is added to the list of repositories tracked by
	 * the repository manager. Clients must make a subsequent call to {@link #removeRepository(URL)}
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
	public IArtifactRepository loadRepository(URL location, IProgressMonitor monitor) throws ProvisionException;

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
	public IArtifactRepository refreshRepository(URL location, IProgressMonitor monitor) throws ProvisionException;

	/**
	 * Remove the given repository from this manager.  Do nothing if the repository
	 * is not currently managed.
	 * 
	 * @param location the location of the repository to remove
	 * @return <code>true</code> if a repository was removed, and 
	 * <code>false</code> otherwise.
	 */
	public boolean removeRepository(URL location);

	/**
	 * Sets the enablement of a repository. Disabled repositories are known
	 * to the repository manager, but are never used in the context of provisioning
	 * operation. Disabled repositories are useful as a form of bookmark to indicate that a 
	 * repository location is of interest, but not currently used.
	 * <p>
	 * Note that enablement is a property of the repository manager and not a property
	 * of the affected repository. The enablement of the repository is discarded when 
	 * a repository is removed from the repository manager.
	 * <p>
	 * This method has no effect if the given repository location is not known to the
	 * repository manager.
	 * 
	 * @param location The location of the repository to enable or disable
	 * @param enablement <code>true</code>to enable the repository, and
	 * <code>false</code> to disable the repository
	 * @see #REPOSITORIES_DISABLED
	 * @see #isEnabled(URL)
	 */
	public void setEnabled(URL location, boolean enablement);
}