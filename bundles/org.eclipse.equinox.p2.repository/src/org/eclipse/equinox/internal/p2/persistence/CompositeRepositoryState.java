/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.persistence;

import java.net.URI;
import java.util.Map;

/*
 * Instances of this class represent a composite repository (either metadata
 * or artifact) and are used in persisting or retrieving the repository to/from disk.
 */
public class CompositeRepositoryState {
	private String name;
	private String type;
	private String version;
	private String provider;
	private String description;
	private URI location;
	private Map<String, String> properties;
	private URI[] children;

	public void setName(String value) {
		name = value;
	}

	public String getName() {
		return name;
	}

	public void setType(String value) {
		type = value;
	}

	public String getType() {
		return type;
	}

	public void setVersion(String value) {
		version = value;
	}

	public String getVersion() {
		return version;
	}

	public void setProvider(String value) {
		provider = value;
	}

	public String getProvider() {
		return provider;
	}

	public void setDescription(String value) {
		description = value;
	}

	public String getDescription() {
		return description;
	}

	public void setLocation(URI value) {
		location = value;
	}

	public URI getLocation() {
		return location;
	}

	public void setProperties(Map<String, String> value) {
		properties = value;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setChildren(URI[] value) {
		children = value;
	}

	public URI[] getChildren() {
		return children;
	}
}