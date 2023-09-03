/*******************************************************************************
 *  Copyright (c) 2007, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Hannes Wellmann - Add IQueryable.contains(T) method and implement overrides where suitable
 *******************************************************************************/
package org.eclipse.equinox.p2.repository;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.query.IQueryable;

/**
 * A p2 repository contains either metadata or artifacts related to software
 * provisioning. This base interface defines properties common to all types
 * of repositories.
 * @param <T> The type of contents contained in this repository
 *
 * @noimplement This interface is not intended to be implemented by clients. Instead the abstract classes implementing this interface should be used.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IRepository<T> extends IAdaptable, IQueryable<T> {
	/**
	 * The key for a boolean property indicating that the repository
	 * is a system repository.  System repositories are implementation details
	 * that are not subject to general access, hidden from the typical user, etc.
	 * This property is never stored in the repository itself, but is instead tracked and
	 * managed by an {@link IRepositoryManager}.
	 * @see IRepositoryManager#getRepositoryProperty(URI, String)
	 */
	String PROP_SYSTEM = "p2.system"; //$NON-NLS-1$

	/**
	 * The key for a boolean property indicating that repository metadata is
	 * stored in compressed form.  A compressed repository will have lower
	 * bandwidth cost to read when remote, but higher processing cost to
	 * uncompress when reading.
	 * @see IRepository#getProperties()
	 */
	String PROP_COMPRESSED = "p2.compressed"; //$NON-NLS-1$

	/**
	 * The key for a string property providing a human-readable name for the repository.
	 * @see IRepositoryManager#getRepositoryProperty(URI, String)
	 * @see IRepository#getProperties()
	 */
	String PROP_NAME = "name"; //$NON-NLS-1$

	/**
	 * The key for a string property providing a user-defined name for the repository.
	 * This property is never stored in the repository itself, but is instead tracked and
	 * managed by an {@link IRepositoryManager}.
	 * @see IRepositoryManager#getRepositoryProperty(URI, String)
	 */
	String PROP_NICKNAME = "p2.nickname"; //$NON-NLS-1$

	/**
	 * The key for a string property providing a human-readable description for the repository.
	 * @see IRepositoryManager#getRepositoryProperty(URI, String)
	 * @see IRepository#getProperties()
	 */
	String PROP_DESCRIPTION = "description"; //$NON-NLS-1$

	/**
	 * The key for a string property providing the common base URL that should
	 * be replaced with the mirror URL.
	 * @see IRepository#getProperties()
	 */
	String PROP_MIRRORS_BASE_URL = "p2.mirrorsBaseURL"; //$NON-NLS-1$

	/**
	 * The key for a string property providing a URL that can return mirrors of this
	 * repository.
	 * @see IRepository#getProperties()
	 */
	String PROP_MIRRORS_URL = "p2.mirrorsURL"; //$NON-NLS-1$

	/**
	 * The key for a string property containing the time when the repository was last modified.
	 * @see IRepository#getProperties()
	 */
	String PROP_TIMESTAMP = "p2.timestamp"; //$NON-NLS-1$

	/**
	 * The key for a string property providing the user name to an authenticated
	 * URL.  This key is used in the secure preference store for repository data.
	 * @see #PREFERENCE_NODE
	 */
	String PROP_USERNAME = "username"; //$NON-NLS-1$

	/**
	 * The key for a string property providing the password to an authenticated
	 * URL.  This key is used in the secure preference store for repository data.
	 * @see #PREFERENCE_NODE
	 */
	String PROP_PASSWORD = "password"; //$NON-NLS-1$

	/**
	 * The node identifier for repository secure preference store.
	 */
	String PREFERENCE_NODE = "org.eclipse.equinox.p2.repository"; //$NON-NLS-1$

	/**
	 * A repository type constant (value 0) representing a metadata repository.
	 */
	int TYPE_METADATA = 0;

	/**
	 * A repository type constant (value 1) representing an artifact repository.
	 */
	int TYPE_ARTIFACT = 1;

	/**
	 * General purpose zero-valued bit mask constant. Useful whenever you need to
	 * supply a bit mask with no bits set.
	 */
	int NONE = 0;

	/**
	 * An option flag constant (value 1) indicating an enabled repository.
	 */
	int ENABLED = 1;

	/**
	 * Returns the location of this repository.
	 * @return the URI representing the repository location.
	 */
	URI getLocation();

	/**
	 * Returns the name of the repository.
	 * @return the name of the repository.
	 */
	String getName();

	/**
	 * Returns a string representing the type of the repository. Note this method
	 * does not indicate the type of repository contents (metadata or artifacts),
	 * but instead the unique fully qualified id representing the repository implementation.
	 * @return the type of the repository.
	 */
	String getType();

	/**
	 * Returns a string representing the version for the repository type.
	 * @return the version of the type of the repository.
	 */
	String getVersion();

	/**
	 * Returns a brief description of the repository.
	 * @return the description of the repository.
	 */
	String getDescription();

	/**
	 * Returns the name of the provider of the repository.
	 * @return the provider of this repository.
	 */
	String getProvider();

	/**
	 * Returns a read-only collection of the properties of the repository.
	 * @return the properties of this repository.
	 */
	Map<String, String> getProperties();

	/**
	 * Returns the repository property with the given key, or <code>null</code>
	 * if no such property is defined
	 * @param key the property key
	 * @return the property value, or <code>null</code>
	 */
	String getProperty(String key);

	/**
	 * Returns the provisioning agent that manages this repository
	 * @return A provisioning agent.
	 */
	IProvisioningAgent getProvisioningAgent();

	/**
	 * Returns <code>true</code> if this repository can be modified, and
	 * <code>false</code> otherwise. Attempts to change the contents of
	 * an unmodifiable repository will fail.
	 * @return whether or not this repository can be modified
	 */
	boolean isModifiable();

	/**
	 * Sets the value of the property with the given key. Returns the old property
	 * associated with that key, if any.  Setting a value of <code>null</code> will
	 * remove the corresponding key from the properties of this repository.
	 *
	 * @param key The property key
	 * @param value The new property value, or <code>null</code> to remove the key
	 * @return The old property value, or <code>null</code> if there was no old value
	 */
	String setProperty(String key, String value);

	/**
	 * Sets the value of the property with the given key. Returns the old property
	 * associated with that key, if any.  Setting a value of <code>null</code> will
	 * remove the corresponding key from the properties of this repository.
	 *
	 * @param key The property key
	 * @param value The new property value, or <code>null</code> to remove the key
	 * @param monitor A progress monitor use to track progress and cancel the operation.  This may
	 * be a long running operation if another process holds the lock on this location
	 * @return The old property value, or <code>null</code> if there was no old value
	 * @since 2.1
	 */
	String setProperty(String key, String value, IProgressMonitor monitor);

	/**
	 * Returns true if this repository contains the given element.
	 *
	 * @param element the element to query
	 * @return true if the given element is already in this repository
	 * @since 2.8
	 */
	@Override
	default boolean contains(T element) {
		return IQueryable.super.contains(element);
	}
}
