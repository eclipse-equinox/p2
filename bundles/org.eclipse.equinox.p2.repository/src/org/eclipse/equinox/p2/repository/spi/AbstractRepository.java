/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource   - Ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.spi;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
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
	private final IProvisioningAgent agent;
	private String description;
	private transient URI location;
	private String name;
	private Map<String, String> properties = new OrderedProperties();
	private String provider;
	private String type;
	private String version;

	/**
	 * Creates a new repository with the given attributes.
	 * 
	 * @param agent the provisioning agent to be used by this repository
	 * @param name the repository name
	 * @param type the repository type
	 * @param version the repository version
	 * @param location the repository location
	 * @param description the repository description
	 * @param provider the repository provider
	 * @param properties the repository properties
	 */
	protected AbstractRepository(IProvisioningAgent agent, String name, String type, String version, URI location, String description, String provider, Map<String, String> properties) {
		this.agent = agent;
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
	 * @return the URI of the repository.
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
	 * {@inheritDoc}
	 */
	public String getProperty(String key) {
		return properties.get(key);
	}

	/**
	 * Returns the name of the provider of the repository.
	 * @return the provider of this repository.
	 */
	public synchronized String getProvider() {
		return provider;
	}

	/**
	 * Returns the provisioning agent used by this repository
	 * 
	 * @return the provisioning agent
	 */
	public IProvisioningAgent getProvisioningAgent() {
		return agent;
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

	/**
	 * {@inheritDoc}
	 */
	public boolean isModifiable() {
		return false;
	}

	/**
	 * Sets the description of this repository
	 * 
	 * @param description the repository description
	 */
	public synchronized void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Sets the name of this repository
	 * 
	 * @param value the repository name
	 */
	public synchronized void setName(String value) {
		this.name = value;
	}

	/**
	 * {@inheritDoc}
	 * @since 2.1
	 */
	public synchronized String setProperty(String key, String value, IProgressMonitor monitor) {
		assertModifiable();
		if (key.equals(IRepository.PROP_NAME)) {
			String oldName = getName();
			setName(value);
			return oldName;
		}
		return (value == null ? properties.remove(key) : properties.put(key, value));
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized String setProperty(String key, String value) {
		return this.setProperty(key, value, new NullProgressMonitor());
	}

	/**
	 * Sets the provider of this repository
	 * 
	 * @param provider the repository provider
	 */
	public synchronized void setProvider(String provider) {
		this.provider = provider;
	}

	/**
	 * Sets the type of this repository
	 * 
	 * @param type the repository type
	 */
	protected synchronized void setType(String type) {
		this.type = type;
	}

	/**
	 * Sets the location of this repository
	 * 
	 * @param location the repository location
	 */
	protected synchronized void setLocation(URI location) {
		this.location = location;
	}

	/**
	 * Sets the version of this repository
	 * 
	 * @param version the repository version
	 */
	protected synchronized void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Sets the properties of this repository
	 * 
	 * @param properties the repository provider
	 */
	protected synchronized void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
}
