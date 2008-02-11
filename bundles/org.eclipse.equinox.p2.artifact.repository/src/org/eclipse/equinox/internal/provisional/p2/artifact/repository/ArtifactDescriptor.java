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
package org.eclipse.equinox.internal.provisional.p2.artifact.repository;

import java.util.Arrays;
import java.util.Map;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;

/**
 * This represents information about a given artifact stored on a particular byte server.
 */
public class ArtifactDescriptor implements IArtifactDescriptor {

	private static final ProcessingStepDescriptor[] EMPTY_STEPS = new ProcessingStepDescriptor[0];

	protected IArtifactKey key; // The key associated with this artifact

	// The list of post processing steps that must be applied one the artifact once it 
	// has been downloaded (e.g, unpack, then md5 checksum, then...)
	protected ProcessingStepDescriptor[] processingSteps = EMPTY_STEPS;

	protected Map properties = new OrderedProperties();
	protected Map repositoryProperties = new OrderedProperties();

	protected transient IArtifactRepository repository;

	// QUESTION: Do we need any description or user readable name

	public ArtifactDescriptor(IArtifactDescriptor base) {
		super();
		key = base.getArtifactKey();
		processingSteps = base.getProcessingSteps();
		properties.putAll(base.getProperties());
		repository = base.getRepository();
		// TODO this property is hardcoded for the blob store.
		//		setProperty("artifact.uuid", base.getProperty("artifact.uuid"));
	}

	public ArtifactDescriptor(ArtifactDescriptor base) {
		super();
		key = base.key;
		processingSteps = base.processingSteps;
		properties = base.properties;
		repository = base.repository;
	}

	public ArtifactDescriptor(IArtifactKey key) {
		super();
		this.key = key;
	}

	public IArtifactKey getArtifactKey() {
		return key;
	}

	public String getProperty(String propertyKey) {
		return (String) properties.get(propertyKey);
	}

	public void setProperty(String key, String value) {
		if (value == null)
			properties.remove(key);
		else
			properties.put(key, value);
	}

	public void addProperties(Map additionalProperties) {
		properties.putAll(additionalProperties);
	}

	/**
	 * Returns a read-only collection of the properties of the artifact descriptor.
	 * @return the properties of this artifact descriptor.
	 */
	public Map getProperties() {
		return OrderedProperties.unmodifiableProperties(properties);
	}

	public String getRepositoryProperty(String propertyKey) {
		return (String) repositoryProperties.get(propertyKey);
	}

	public void setRepositoryProperty(String key, String value) {
		if (value == null)
			repositoryProperties.remove(key);
		else
			repositoryProperties.put(key, value);
	}

	public void addRepositoryProperties(Map additionalProperties) {
		repositoryProperties.putAll(additionalProperties);
	}

	/**
	 * Returns a read-only collection of the repository properties of the artifact descriptor.
	 * @return the repository properties of this artifact descriptor.
	 */
	public Map getRepositoryProperties() {
		return OrderedProperties.unmodifiableProperties(repositoryProperties);
	}

	public ProcessingStepDescriptor[] getProcessingSteps() {
		return processingSteps;
	}

	public void setProcessingSteps(ProcessingStepDescriptor[] value) {
		processingSteps = value == null ? EMPTY_STEPS : value;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArtifactDescriptor other = (ArtifactDescriptor) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (!Arrays.equals(processingSteps, other.processingSteps))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		return true;
	}

	private int hashCode(Object[] array) {
		int prime = 31;
		if (array == null)
			return 0;
		int result = 1;
		for (int index = 0; index < array.length; index++) {
			result = prime * result + (array[index] == null ? 0 : array[index].hashCode());
		}
		return result;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + hashCode(processingSteps);
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
		return result;
	}

	public IArtifactRepository getRepository() {
		return repository;
	}

	public void setRepository(IArtifactRepository value) {
		repository = value;
	}

	public String toString() {
		String format = getProperty(IArtifactDescriptor.FORMAT);
		if (format == null)
			return "canonical: " + key.toString(); //$NON-NLS-1$
		return format + ": " + key.toString(); //$NON-NLS-1$
	}

}
