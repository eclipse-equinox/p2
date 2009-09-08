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
package org.eclipse.equinox.internal.provisional.p2.engine;

import java.util.Map;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

/**
 * This encapsulates the access to the profile registry. 
 * It deals with persistence in a transparent way.
 */
public interface IProfileRegistry {
	public static final String SELF = "_SELF_"; //$NON-NLS-1$

	/**
	 * Return the profile in the registry that has the given id. If it does not exist, 
	 * then return <code>null</code>.
	 * 
	 * @param id the profile identifier
	 * @return the profile or <code>null</code>
	 */
	IProfile getProfile(String id);

	/**
	 * Return the profile in the registry that has the given id and timestamp. If it does not exist, 
	 * then return <code>null</code>.
	 * 
	 * @param id the profile identifier
	 * @param timestamp the profile's timestamp

	 * @return the profile or <code>null</code>
	 */
	IProfile getProfile(String id, long timestamp);

	/**
	 * Return an array of timestamps in ascending order for the profile in question. If there are none, then
	 * return an empty array.
	 * 
	 * @return the array of timestamps
	 */
	long[] listProfileTimestamps(String id);

	/**
	 * Return an array of profiles known to this registry. If there are none, then
	 * return an empty array.
	 * 
	 * @return the array of profiles
	 */
	IProfile[] getProfiles();

	/**
	 * Add the given profile to this profile registry.
	 * 
	 * @param id the profile id
	 * 
	 * @throws ProvisionException if a profile
	 *         with the same id is already present in the registry.
	 */
	IProfile addProfile(String id) throws ProvisionException;

	/**
	 * Add the given profile to this profile registry.
	 * 
	 * @param id the profile id
	 * @param properties the profile properties
	 * 
	 * @throws ProvisionException if a profile
	 *         with the same id is already present in the registry.
	 */
	IProfile addProfile(String id, Map properties) throws ProvisionException;

	/**
	 * Add the given profile to this profile registry.
	 * 
	 * @param id the profile id
	 * @param properties the profile properties
	 * @param parentId the id of a parent profile
	 * 
	 * @throws ProvisionException if a profile
	 *         with the same id is already present in the registry or the parentId is not a registered Profile.
	 */
	IProfile addProfile(String id, Map properties, String parentId) throws ProvisionException;

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
	 * 
	 * @throws ProvisionException if the profile with the specified id and timestamp is the current profile.
	 */
	void removeProfile(String id, long timestamp) throws ProvisionException;

	/**
	 * Remove the given profile from this profile registry.  This method has no effect
	 * if this registry does not contain a profile with the given id.
	 * 
	 * @param id the profile to remove
	 */
	void removeProfile(String id);

	/**
	 * Check if the given profile from this profile registry is up-to-date.
	 * 
	 * @param profile the profile to check
	 * @return boolean  true if the profile is current; false otherwise.
	 */
	public boolean isCurrent(IProfile profile);
}
