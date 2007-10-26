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
package org.eclipse.equinox.spi.p2.core.repository;

import java.net.URL;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.equinox.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.core.helpers.UnmodifiableProperties;
import org.eclipse.equinox.p2.core.repository.IRepository;

/**
* AbstractRepository defines common properties that may be provided by various kinds
* of repositories.
* <p>
* Clients may extend this class.
* </p>
* TODO: Do we want additional properties - time zone, copyrights, security etc.. 
*/

public abstract class AbstractRepository extends PlatformObject implements IRepository {
	protected String name;
	protected String type;
	protected String version;
	protected String description;
	protected String provider;
	protected transient URL location;
	protected OrderedProperties properties = new OrderedProperties();

	protected AbstractRepository(String name, String type, String version, URL location, String description, String provider) {
		this.name = name;
		this.type = type;
		this.version = version;
		this.location = location;
		this.description = description == null ? "" : description; //$NON-NLS-1$
		this.provider = provider == null ? "" : provider; //$NON-NLS-1$
	}

	/**
	 * Returns the name of the repository.
	 * @return the name of the repository.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns a string representing the type of the repository.
	 * @return the type of the repository.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns a string representing the version for the repository type.
	 * @return the version of the type of the repository.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Returns the location of this repository.
	 * TODO: Should we use URL or URI? URL requires a protocol handler
	 * to be installed in Java.  Can the URL have any protocol?
	 * @return the URL of the repository.
	 */
	public URL getLocation() {
		return location;
	}

	/**
	 * Returns a brief description of the repository.
	 * @return the description of the repository.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the name of the provider of the repository.
	 * @return the provider of this repository.
	 */
	public String getProvider() {
		return provider;
	}

	/**
	 * Returns a read-only collection of the properties of the repository.
	 * @return the properties of this repository.
	 */
	public UnmodifiableProperties getProperties() {
		return new UnmodifiableProperties(properties);
	}

	public void setName(String value) {
		name = value;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public OrderedProperties getModifiableProperties() {
		return properties;
	}

	public boolean isModifiable() {
		return false;
	}
}
