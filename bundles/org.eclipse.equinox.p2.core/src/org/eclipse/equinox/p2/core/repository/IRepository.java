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
package org.eclipse.equinox.p2.core.repository;

import java.net.URL;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.equinox.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.core.helpers.UnmodifiableProperties;

/**
 * IRepositoryInfo defines common properties that may be provided by various kinds
 * of repositories.
 * <p>
 * Clients may implement this interface.
 * </p>
 * TODO: This should be an abstract class so methods can be added in the future
 *       without breaking clients.
 * TODO: Do we want additional properties - time zone, copyrights, security etc.. 
 */
public interface IRepository extends IAdaptable {
	// The property key for a boolean property indicating that the repository
	// is an implementation detail, not subject to general access, hidden
	// from the typical user, etc.
	static public String IMPLEMENTATION_ONLY_KEY = "implementationOnly"; //$NON-NLS-1$

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
	public UnmodifiableProperties getProperties();

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
	 * Returns a brief description of the repository.
	 */
	public void setDescription(String description);

	/**
	 * Set the name of the provider of the repository.
	 */
	public void setProvider(String provider);

	/**
	 * Returns the modifiable collection of the properties of the repository.
	 * @return the properties of this repository.
	 */
	public OrderedProperties getModifiableProperties();
}
