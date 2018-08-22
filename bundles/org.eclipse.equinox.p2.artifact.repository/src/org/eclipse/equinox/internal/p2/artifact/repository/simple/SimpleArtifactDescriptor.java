/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository.simple;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;

public class SimpleArtifactDescriptor extends ArtifactDescriptor {
	public static final String ARTIFACT_REFERENCE = "artifact.reference"; //$NON-NLS-1$

	protected Map<String, String> repositoryProperties;

	public SimpleArtifactDescriptor(IArtifactKey key) {
		super(key);
	}

	public SimpleArtifactDescriptor(IArtifactDescriptor base) {
		super(base);
	}

	public String getRepositoryProperty(String propertyKey) {
		return repositoryProperties != null ? repositoryProperties.get(propertyKey) : null;
	}

	public void setRepositoryProperty(String key, String value) {
		if (value == null) {
			if (repositoryProperties != null) {
				repositoryProperties.remove(key);
				if (repositoryProperties.isEmpty()) {
					repositoryProperties = null;
				}
			}
		} else {
			if (repositoryProperties == null) {
				// first value => store in singletonMap (most repositoryProperties have at most 1 entry)
				repositoryProperties = Collections.singletonMap(key.intern(), value);
			} else {
				// if current size is 1 then it is an immutable singletonMap 
				// => copy to mutable map for more entries
				if (repositoryProperties.size() == 1) {
					repositoryProperties = new OrderedProperties(repositoryProperties);
				}
				repositoryProperties.put(key, value);
			}
		}
	}

	public void addRepositoryProperties(Map<String, String> additionalProperties) {
		if (additionalProperties.isEmpty())
			return;
		for (Entry<String, String> entry : additionalProperties.entrySet()) {
			setRepositoryProperty(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Returns a read-only collection of the repository properties of the artifact descriptor.
	 * @return the repository properties of this artifact descriptor.
	 */
	public Map<String, String> getRepositoryProperties() {
		if (repositoryProperties == null) {
			return Collections.<String, String> emptyMap();
		}
		return OrderedProperties.unmodifiableProperties(repositoryProperties);
	}

	@Override
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

	@Override
	public int hashCode() {
		int superHash = super.hashCode();
		String ref = getRepositoryProperty(ARTIFACT_REFERENCE);
		if (ref != null)
			return 31 * superHash + ref.hashCode();
		return superHash;
	}
}
