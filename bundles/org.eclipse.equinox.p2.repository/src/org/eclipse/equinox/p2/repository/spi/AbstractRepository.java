/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.spi;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.repository.IRepository;

/**
* AbstractRepository defines common properties that may be provided by various kinds
* of repositories.
* <p>
* Clients may extend this class.
* </p>
* @param <T> the type of object that can be queried for in this repository
 * @since 2.0
*/
public abstract class AbstractRepository<T> extends PlatformObject implements IRepository<T> {
	protected String description;
	protected transient URI location;
	protected String name;
	protected Map<String, String> properties = new OrderedProperties();
	protected String provider;
	protected String type;
	protected String version;

	protected AbstractRepository(String name, String type, String version, URI location, String description, String provider, Map<String, String> properties) {
		this.name = name;
		this.type = type;
		this.version = version;
		this.location = location;
		this.description = description == null ? "" : description; //$NON-NLS-1$
		this.provider = provider == null ? "" : provider; //$NON-NLS-1$
		if (properties != null)
			this.properties.putAll(properties);
	}

	/**
	 * Asserts that this repository is modifiable, throwing a runtime exception if
	 * it is not. This is suitable for use by subclasses when an attempt is made
	 * to write to a repository.
	 */
	protected void assertModifiable() {
		if (!isModifiable())
			throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	/**
	 * Returns a brief description of the repository.
	 * @return the description of the repository.
	 */
	public synchronized String getDescription() {
		return description;
	}

	/**
	 * Returns the location of this repository.
	 * TODO: Should we use URL or URI? URL requires a protocol handler
	 * to be installed in Java.  Can the URL have any protocol?
	 * @return the URL of the repository.
	 */
	public synchronized URI getLocation() {
		return location;
	}

	/**
	 * Returns the name of the repository.
	 * @return the name of the repository.
	 */
	public synchronized String getName() {
		return name;
	}

	/**
	 * Returns a read-only collection of the properties of the repository.
	 * @return the properties of this repository.
	 */
	public synchronized Map<String, String> getProperties() {
		return OrderedProperties.unmodifiableProperties(properties);
	}

	/**
	 * Returns the name of the provider of the repository.
	 * @return the provider of this repository.
	 */
	public synchronized String getProvider() {
		return provider;
	}

	/**
	 * Returns a string representing the type of the repository.
	 * @return the type of the repository.
	 */
	public synchronized String getType() {
		return type;
	}

	/**
	 * Returns a string representing the version for the repository type.
	 * @return the version of the type of the repository.
	 */
	public synchronized String getVersion() {
		return version;
	}

	public boolean isModifiable() {
		return false;
	}

	public synchronized void setDescription(String description) {
		this.description = description;
	}

	public synchronized void setName(String value) {
		this.name = value;
	}

	public synchronized String setProperty(String key, String value) {
		assertModifiable();
		return (value == null ? properties.remove(key) : properties.put(key, value));
	}

	public synchronized void setProvider(String provider) {
		this.provider = provider;
	}
}
