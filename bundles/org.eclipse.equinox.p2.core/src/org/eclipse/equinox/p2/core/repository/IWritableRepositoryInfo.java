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

import org.eclipse.equinox.p2.core.helpers.OrderedProperties;

/**
 * IWritableRepositoryInfo provides setters for common properties of various kinds
 * of repositories.
 * <p>
 * Clients may implement this interface.
 */
public interface IWritableRepositoryInfo extends IRepositoryInfo {

	/**
	 * Set the name of the repository.
	 */
	public void setName(String name);

	/**
	 * Set the type of the repository.
	 */
	// TODO remove this method
	public void setType(String type);

	/**
	 * Set the string representing the version for the repository type.
	 */
	// TODO and likely this method too
	public void setVersion(String version);

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
