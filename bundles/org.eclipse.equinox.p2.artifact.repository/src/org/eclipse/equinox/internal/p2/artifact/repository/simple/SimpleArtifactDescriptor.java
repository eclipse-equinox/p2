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
package org.eclipse.equinox.internal.p2.artifact.repository.simple;

import java.util.Map;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;

public class SimpleArtifactDescriptor extends ArtifactDescriptor {
	public static final String ARTIFACT_REFERENCE = "artifact.reference"; //$NON-NLS-1$

	protected Map<String, String> repositoryProperties = new OrderedProperties();

	public SimpleArtifactDescriptor(IArtifactKey key) {
		super(key);
	}

	public SimpleArtifactDescriptor(IArtifactDescriptor base) {
		super(base);
	}

	public String getRepositoryProperty(String propertyKey) {
		return repositoryProperties.get(propertyKey);
	}

	public void setRepositoryProperty(String key, String value) {
		if (value == null)
			repositoryProperties.remove(key);
		else
			repositoryProperties.put(key, value);
	}

	public void addRepositoryProperties(Map<String, String> additionalProperties) {
		repositoryProperties.putAll(additionalProperties);
	}

	/**
	 * Returns a read-only collection of the repository properties of the artifact descriptor.
	 * @return the repository properties of this artifact descriptor.
	 */
	public Map<String, String> getRepositoryProperties() {
		return OrderedProperties.unmodifiableProperties(repositoryProperties);
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !(obj instanceof SimpleArtifactDescriptor))
			return false;

		SimpleArtifactDescriptor other = (SimpleArtifactDescriptor) obj;

		//Properties affecting SimpleArtifactRepository#getLocation
		String locationProperty = getRepositoryProperty(ARTIFACT_REFERENCE);
		String otherProperty = other.getRepositoryProperty(ARTIFACT_REFERENCE);
		// want not null and the same, or both null
		if (locationProperty != null ? !locationProperty.equals(otherProperty) : otherProperty != null)
			return false;

		return super.equals(obj);
	}

	public int hashCode() {
		int superHash = super.hashCode();
		String ref = getRepositoryProperty(ARTIFACT_REFERENCE);
		if (ref != null)
			return 31 * superHash + ref.hashCode();
		return superHash;
	}
}
