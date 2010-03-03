/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.util.Map;
import org.eclipse.equinox.p2.core.ProvisionException;

/**
 * This encapsulates the access to the profile registry. 
 * It deals with persistence in a transparent way.
 * @since 2.0
 */
public interface IProfileRegistry {
	/**
	 * A special profile id representing the profile of the currently running system.
	 * This constant can be used when invoking {@link #getProfile(String)} to obtain
	 * the profile of the currently running system. Note that a given profile registry
	 * may not have a defined self profile, for example if the running system doesn't
	 * have a profile, or resides in a different profile registry.
	 */
	public static final String SELF = "_SELF_"; //$NON-NLS-1$
	/**
	 * Service name constant for the profile registry service.
	 */
	public static final String SERVICE_NAME = IProfileRegistry.class.getName();

	/**
	 * Return the profile in the registry that has the given id. If it does not exist, 
	 * then return <code>null</code>.
	 * 
	 * @param id the profile identifier
	 * @return the profile or <code>null</code>
	 */
	public IProfile getProfile(String id);

	/**
	 * Return the profile in the registry that has the given id and timestamp. If it does not exist, 
	 * then return <code>null</code>.
	 * 
	 * @param id the profile identifier
	 * @param timestamp the profile's timestamp
	 * @return the profile or <code>null</code>
	 */
	public IProfile getProfile(String id, long timestamp);

	/**
	 * Return an array of timestamps in ascending order for the profile id in question. 
	 * If there are none, then return an empty array.
	 * 
	 * @param id the id of the profile to list timestamps for
	 * @return the array of timestamps
	 */
	public long[] listProfileTimestamps(String id);

	/**
	 * Return an array of profiles known to this registry. If there are none, then
	 * return an empty array.
	 * 
	 * @return the array of profiles
	 */
	public IProfile[] getProfiles();

	/**
	 * Add the given profile to this profile registry.
	 * 
	 * @param id the profile id
	 * @throws ProvisionException if a profile
	 *         with the same id is already present in the registry.
	 * @return the new empty profile
	 */
	public IProfile addProfile(String id) throws ProvisionException;

	/**
	 * Add the given profile to this profile registry.
	 * 
	 * @param id the profile id
	 * @param properties the profile properties
	 * @throws ProvisionException if a profile
	 *         with the same id is already present in the registry.
	 * @return the new empty profile
	 */
	public IProfile addProfile(String id, Map<String, String> properties) throws ProvisionException;

	/**
	 * Returns whether this profile registry contains a profile with the given id.
	 * 
	 * @param profileId The id of the profile to search for
	 * @return <code>true</code> if this registry contains a profile with the given id,
	 * and <code>false</code> otherwise.
	 */
	public boolean containsProfile(String profileId);

	/**
	 * Remove the given profile snapshot from this profile registry. This method has no effect
	 * if this registry does not contain a profile with the given id and timestamp.
	 * The current profile cannot be removed using this method.
	 * 
	 * @param id the profile to remove
	 * @param timestamp the timestamp of the profile to remove 
	 * @throws ProvisionException if the profile with the specified id and timestamp is the current profile.
	 */
	public void removeProfile(String id, long timestamp) throws ProvisionException;

	/**
	 * Remove the given profile from this profile registry.  This method has no effect
	 * if this registry does not contain a profile with the given id.
	 * 
	 * @param id the profile to remove
	 */
	public void removeProfile(String id);

	/**
	 * Check if the given profile from this profile registry is up-to-date.
	 * 
	 * @param profile the profile to check
	 * @return boolean  true if the profile is current; false otherwise.
	 */
	public boolean isCurrent(IProfile profile);
}
