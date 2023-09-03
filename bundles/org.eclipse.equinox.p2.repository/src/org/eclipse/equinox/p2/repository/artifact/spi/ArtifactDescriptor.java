/*******************************************************************************
 * Copyright (c) 2007, 2023 IBM Corporation and others.
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
 *     Cloudsmith Inc. - IMemberProvider access.
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.artifact.spi;

import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.expression.IMemberProvider;
import org.eclipse.equinox.p2.repository.artifact.*;

/**
 * This represents information about a given artifact stored on a particular byte server.
 * @since 2.0
 */
public class ArtifactDescriptor implements IArtifactDescriptor, IMemberProvider {
	public static final String MEMBER_ARTIFACT_KEY = "artifactKey"; //$NON-NLS-1$
	public static final String MEMBER_PROCESSING_STEPS = "processingSteps"; //$NON-NLS-1$
	public static final String MEMBER_PROPERTIES = "properties"; //$NON-NLS-1$
	public static final String MEMBER_REPOSITORY = "repository"; //$NON-NLS-1$

	private static final IProcessingStepDescriptor[] EMPTY_STEPS = new ProcessingStepDescriptor[0];

	protected IArtifactKey key; // The key associated with this artifact

	// The list of post processing steps that must be applied one the artifact once it
	// has been downloaded (e.g, md5 checksum, then...)
	protected IProcessingStepDescriptor[] processingSteps = EMPTY_STEPS;

	protected Map<String, String> properties = new OrderedProperties();

	private transient IArtifactRepository repository;

	/**
	 * Creates a new artifact descriptor with the same key, properties, repository,
	 * and processing steps as the provided base descriptor.
	 *
	 * @param base the descriptor to use as a template for this new descriptor
	 */
	public ArtifactDescriptor(IArtifactDescriptor base) {
		key = base.getArtifactKey();
		setProcessingSteps(base.getProcessingSteps());
		properties.putAll(base.getProperties());
		repository = base.getRepository();
	}

	/**
	 * Returns a new artifact descriptor that uses the provided artifact key
	 *
	 * @param key The artifact key corresponding to this descriptor
	 */
	public ArtifactDescriptor(IArtifactKey key) {
		this.key = key;
	}

	@Override
	public IArtifactKey getArtifactKey() {
		return key;
	}

	@Override
	public String getProperty(String propertyKey) {
		return properties.get(propertyKey);
	}

	public void setProperty(String key, String value) {
		if (value == null) {
			properties.remove(key);
		} else {
			properties.put(key, value);
		}
	}

	public void addProperties(Map<String, String> additionalProperties) {
		properties.putAll(additionalProperties);
	}

	/**
	 * Returns a read-only collection of the properties of the artifact descriptor.
	 * @return the properties of this artifact descriptor.
	 */
	@Override
	public Map<String, String> getProperties() {
		return OrderedProperties.unmodifiableProperties(properties);
	}

	@Override
	public IProcessingStepDescriptor[] getProcessingSteps() {
		return processingSteps;
	}

	public void setProcessingSteps(IProcessingStepDescriptor[] value) {
		processingSteps = value == null || value.length == 0 ? EMPTY_STEPS : value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		// Other implementations of IArtifactDescriptor must not be considered equal
		if (obj == null || !(obj.getClass().equals(getClass()))) {
			return false;
		}
		ArtifactDescriptor other = (ArtifactDescriptor) obj;
		return Objects.equals(key, other.getArtifactKey()) //
				&& Arrays.equals(processingSteps, other.getProcessingSteps())
				&& Objects.equals(getProperty(FORMAT), other.getProperty(FORMAT));
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, Arrays.asList(processingSteps), getProperty(FORMAT));
	}

	@Override
	public IArtifactRepository getRepository() {
		return repository;
	}

	public void setRepository(IArtifactRepository value) {
		repository = value;
	}

	@Override
	public String toString() {
		String format = getProperty(IArtifactDescriptor.FORMAT);
		if (format == null) {
			return "canonical: " + key.toString(); //$NON-NLS-1$
		}
		return format + ": " + key.toString(); //$NON-NLS-1$
	}

	@Override
	public Object getMember(String memberName) {
		return switch (memberName) {
		case MEMBER_ARTIFACT_KEY -> key;
		case MEMBER_PROPERTIES -> properties;
		case MEMBER_PROCESSING_STEPS -> processingSteps;
		case MEMBER_REPOSITORY -> repository;
		default -> throw new IllegalArgumentException("No such member: " + memberName); //$NON-NLS-1$
		};
	}

}
