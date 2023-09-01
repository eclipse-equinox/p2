/*******************************************************************************
 * Copyright (c) 2008, 2023 IBM Corporation and others.
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
 *     Hannes Wellmann - Generalize create/load/refereshRepository() into IRepositoryManager
 *******************************************************************************/
package org.eclipse.equinox.p2.repository;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.query.IQueryable;

/**
 * The common base class for metadata and artifact repository managers.
 * <p>
 * A repository manager keeps track of a set of known repositories, and provides
 * caching of these known repositories to avoid unnecessary loading of
 * repositories from the disk or network.
 * </p>
 * <p>
 * All {@link URI} instances provided to a repository manager must be absolute.
 * </p>
 *
 * @param <T> the type of contents in the repositories controlled by this manager
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IRepositoryManager<T> extends IQueryable<T> {
	/**
	 * Constant used to indicate that all enabled repositories are of interest.
	 */
	int REPOSITORIES_ALL = 0;

	/**
	 * Constant used to indicate that only system repositories are of interest.
	 *
	 * @see IRepository#PROP_SYSTEM
	 * @see #getKnownRepositories(int)
	 */
	int REPOSITORIES_SYSTEM = 1 << 0;

	/**
	 * Constant used to indicate that only non-system repositories are of interest.
	 *
	 * @see IRepository#PROP_SYSTEM
	 * @see #getKnownRepositories(int)
	 */
	int REPOSITORIES_NON_SYSTEM = 1 << 1;

	/**
	 * Constant used to indicate that only local repositories are of interest. Any
	 * repository that requires network communication will be omitted when
	 * this flag is used.
	 *
	 * @see #getKnownRepositories(int)
	 */
	int REPOSITORIES_LOCAL = 1 << 2;

	/**
	 * Constant used to indicate that only remote repositories are of interest. Any
	 * repository that doesn't require network communication will be omitted when
	 * this flag is used.
	 *
	 * @see #getKnownRepositories(int)
	 */
	int REPOSITORIES_NON_LOCAL = 1 << 4;

	/**
	 * Constant used to indicate that only disabled repositories are of interest.
	 * When this flag is used, all enabled repositories will be ignored and
	 * all disabled repositories that match the remaining filters will be returned.
	 *
	 * @see #getKnownRepositories(int)
	 */
	int REPOSITORIES_DISABLED = 1 << 3;

	/**
	 * Constant used to indicate that a repository manager should only load the
	 * repository if the repository is modifiable.
	 *
	 * @see IRepository#isModifiable()
	 */
	int REPOSITORY_HINT_MODIFIABLE = 1 << 0;

	/**
	 * Adds the repository at the given location to the list of repositories tracked by
	 * this repository manager. This method does not attempt to contact or load
	 * the repository, and makes no attempt to determine whether there is a valid
	 * repository at the provided location.
	 * <p>
	 * If there is a known disabled repository at the given location, it will become
	 * enabled as a result of this method. Thus the caller can be guaranteed that
	 * there is a known, enabled repository at the given location when this method returns.
	 *
	 * @param location The absolute location of the repository to add
	 * @see #isEnabled(URI)
	 */
	void addRepository(URI location);

	/**
	 * Returns whether a repository at the given location is in the list of repositories
	 * tracked by this repository manager.
	 *
	 * @param location The absolute location of the repository to look for
	 * @return <code>true</code> if the repository is known to this manager,
	 * and <code>false</code> otherwise
	 */
	boolean contains(URI location);

	/**
	 * Returns the provisioning agent in charge of this repository manager
	 *
	 * @return The provisioning agent.
	 */
	IProvisioningAgent getAgent();

	/**
	 * Returns the repository locations known to the repository manager.
	 * <p>
	 * Note that the repository manager does not guarantee that a valid repository
	 * exists at any of the returned locations at any particular moment in time.
	 * A subsequent attempt to load a repository at any of the given locations may
	 * or may not succeed.
	 *
	 * @param flags an integer bit-mask indicating which repositories should be
	 * returned. <code>REPOSITORIES_ALL</code> can be used as the mask when
	 * all enabled repositories should be returned. Disabled repositories are automatically
	 * excluded unless the {@link #REPOSITORIES_DISABLED} flag is set.
	 * @return the locations of the repositories managed by this repository manager.
	 *
	 * @see #REPOSITORIES_ALL
	 * @see #REPOSITORIES_SYSTEM
	 * @see #REPOSITORIES_NON_SYSTEM
	 * @see #REPOSITORIES_LOCAL
	 * @see #REPOSITORIES_DISABLED
	 */
	URI[] getKnownRepositories(int flags);

	/**
	 * Returns the property associated with the repository at the given URI,
	 * without loading the repository.
	 * <p>
	 * Note that only the repository properties referenced below are tracked by the
	 * repository manager itself. For all other properties, this method will return <code>null</code>.
	 * Only values for the properties that are already known by a repository manager will be returned.
	 * </p>
	 * <p>
	 * If a client wishes to retrieve a property value from a repository
	 * regardless of the cost of retrieving it, the client should load the
	 * repository and then retrieve the property from the repository itself.
	 * </p>
	 *
	 * @param location the absolute URI of the repository in question
	 * @param key the String key of the property desired
	 * @return the value of the property, or <code>null</code> if the repository
	 * does not exist, the value does not exist, or the property value
	 * could not be determined without loading the repository.
	 *
	 * @see IRepository#getProperties()
	 * @see #setRepositoryProperty(URI, String, String)
	 * @see IRepository#PROP_NAME
	 * @see IRepository#PROP_NICKNAME
	 * @see IRepository#PROP_DESCRIPTION
	 * @see IRepository#PROP_SYSTEM
	 */
	String getRepositoryProperty(URI location, String key);

	/**
	 * Sets the property associated with the repository at the given URI,
	 * without loading the repository.
	 * <p>
	 * This method stores properties in a cache in the repository manager and does
	 * not write the property to the backing repository. This is useful for making
	 * repository properties available without incurring the cost of loading the repository.
	 * When the repository is loaded, it will overwrite any conflicting properties that
	 * have been set using this method. Only the repository properties referenced
	 * below can be stored by the repository manager; attempts to set other
	 * repository properties will be ignored.
	 * </p>
	 * <p>
	 * To persistently set a property on a repository, clients must load
	 * the repository and call {@link IRepository#setProperty(String, String)}.
	 * </p>
	 *
	 * @param location the absolute URI of the repository in question
	 * @param key the String key of the property desired
	 * @param value the value to set the property to
	 * @see #getRepositoryProperty(URI, String)
	 * @see IRepository#setProperty(String, String)
	 * @see IRepository#PROP_NAME
	 * @see IRepository#PROP_NICKNAME
	 * @see IRepository#PROP_DESCRIPTION
	 * @see IRepository#PROP_SYSTEM
	 */
	void setRepositoryProperty(URI location, String key, String value);

	/**
	 * Returns the enablement value of a repository. Disabled repositories are known
	 * to the repository manager, but are never used in the context of provisioning
	 * operations. Disabled repositories are useful as a form of bookmark to indicate that a
	 * repository location is of interest, but not currently used.
	 * <p>
	 * Note that enablement is a property of the repository manager and not a property
	 * of the affected repository. The enablement of the repository is discarded when
	 * a repository is removed from the repository manager.
	 *
	 * @param location The absolute location of the repository whose enablement is requested
	 * @return <code>true</code> if the repository is enabled, and
	 * <code>false</code> if it is not enabled, or if the repository location
	 * is not known to the repository manager.
	 * @see #REPOSITORIES_DISABLED
	 * @see #setEnabled(URI, boolean)
	 */
	boolean isEnabled(URI location);

	/**
	 * Removes the repository at the given location from the list of
	 * repositories known to this repository manager. The underlying
	 * repository is not deleted. This method has no effect if the given
	 * repository is not already known to this repository manager.
	 *
	 * @param location The absolute location of the repository to remove
	 * @return <code>true</code> if a repository was removed, and
	 * <code>false</code> otherwise.
	 */
	boolean removeRepository(URI location);

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
	 * @param location The absolute location of the repository to enable or disable
	 * @param enablement <code>true</code>to enable the repository, and
	 * <code>false</code> to disable the repository
	 * @see #REPOSITORIES_DISABLED
	 * @see #isEnabled(URI)
	 */
	void setEnabled(URI location, boolean enablement);


	/**
	 * Creates and returns a new empty repository of the given type at the given location.
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
	 * @since 2.8
	 */
	IRepository<T> createRepository(URI location, String name, String type, Map<String, String> properties)
			throws ProvisionException;

	/**
	 * Loads the repository at the given location. If a repository has
	 * previously been loaded at that location, the same cached repository
	 * may be returned.
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
	 * @return the loaded repository
	 * @throws ProvisionException if the repository could not be created. Reasons include:
	 * <ul>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 * @since 2.8
	 */
	IRepository<T> loadRepository(URI location, IProgressMonitor monitor) throws ProvisionException;

	/**
	 * Loads the repository at the given location. If a repository has
	 * previously been loaded at that location, the same cached repository may be returned.
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
	 * @return the loaded repository
	 * @throws ProvisionException if the repository could not be created. Reasons include:
	 * <ul>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 * @see IRepositoryManager#REPOSITORY_HINT_MODIFIABLE
	 * @since 2.8
	 */
	IRepository<T> loadRepository(URI location, int flags, IProgressMonitor monitor) throws ProvisionException;

	/**
	 * Refreshes the repository corresponding to the given URL. This method discards
	 * any cached state held by the repository manager and reloads the repository
	 * contents. The provided repository location must already be known to the repository
	 * manager.
	 *
	 * @param location The absolute location of the repository to refresh
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The refreshed repository
	 * @throws ProvisionException if the repository could not be refreshed. Reasons include:
	 * <ul>
	 * <li>The location is not known to the repository manager.</li>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 * @since 2.8
	 */
	IRepository<T> refreshRepository(URI location, IProgressMonitor monitor) throws ProvisionException;


}
