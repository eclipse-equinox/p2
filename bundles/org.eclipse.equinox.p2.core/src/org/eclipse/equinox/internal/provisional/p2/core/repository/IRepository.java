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
package org.eclipse.equinox.internal.provisional.p2.core.repository;

import java.net.URL;
import java.util.Map;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Base interface that defines common properties that may be provided by 
 * various kinds of repositories.
 * <p>
 * This interface is not intended to be implemented by clients.
 */
public interface IRepository extends IAdaptable {
	/** 
	 * The key for a boolean property indicating that the repository
	 * is a system repository.  System repositories are implementation details
	 * that are not subject to general access, hidden from the typical user, etc.
	 */
	static public String PROP_SYSTEM = "p2.system"; //$NON-NLS-1$

	/**
	 * The key for a boolean property indicating that repository metadata is
	 * stored in compressed form.  A compressed repository will have lower
	 * bandwidth cost to read when remote, but higher processing cost to
	 * uncompress when reading.
	 */
	public static final String PROP_COMPRESSED = "p2.compressed"; //$NON-NLS-1$

	/**
	 * The key for a string property providing a human-readable name for the repository.
	 */
	public static final String PROP_NAME = "name"; //$NON-NLS-1$

	/**
	 * The key for a string property providing a human-readable description for the repository.
	 */
	public static final String PROP_DESCRIPTION = "description"; //$NON-NLS-1$

	/**
	 * The key for a string property providing a URL that can return mirrors of this
	 * repository.
	 */
	public static final String PROP_MIRRORS_URL = "p2.mirrorsURL"; //$NON-NLS-1$

	/**
	 * Returns the URL of the repository.
	 * TODO: Should we use URL or URI? URL requires a protocol handler
	 * to be installed in Java.  Can the URL have any protocol?
	 * @return the URL of the repository.
	 */
	public URL getLocation();

	/**
	 * Returns the name of the repository.
	 * @return the name of the repository.
	 */
	public String getName();

	/**
	 * Returns a string representing the type of the repository.
	 * @return the type of the repository.
	 */
	public String getType();

	/**
	 * Returns a string representing the version for the repository type.
	 * @return the version of the type of the repository.
	 */
	public String getVersion();

	/**
	 * Returns a brief description of the repository.
	 * @return the description of the repository.
	 */
	public String getDescription();

	/**
	 * Returns the name of the provider of the repository.
	 * @return the provider of this repository.
	 */
	public String getProvider();

	/**
	 * Returns a read-only collection of the properties of the repository.
	 * @return the properties of this repository.
	 */
	public Map getProperties();

	/**
	 * Returns <code>true</code> if this repository can be modified.
	 * @return whether or not this repository can be modified
	 */
	public boolean isModifiable();

	/**
	 * Set the name of the repository.
	 */
	public void setName(String name);

	/**
	 * Sets the description of the repository.
	 */
	public void setDescription(String description);

	/**
	 * Sets the value of the property with the given key. Returns the old property
	 * associated with that key, if any.  Setting a value of <code>null</code> will
	 * remove the corresponding key from the properties of this repository.
	 * 
	 * @param key The property key
	 * @param value The new property value, or <code>null</code> to remove the key
	 * @return The old property value, or <code>null</code> if there was no old value
	 */
	public String setProperty(String key, String value);

	/**
	 * Sets the name of the provider of the repository.
	 */
	public void setProvider(String provider);
}
