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
package org.eclipse.equinox.p2.artifact.repository;

import java.util.*;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

/**
 * This represents information about a given artifact stored on a particular byte server.
 */
public class ArtifactDescriptor implements IArtifactDescriptor {

	private static final ProcessingStepDescriptor[] EMPTY_STEPS = new ProcessingStepDescriptor[0];

	protected IArtifactKey key; // The key associated with this artifact

	// The list of post processing steps that must be applied one the artifact once it 
	// has been downloaded (e.g, unpack, then md5 checksum, then...)
	protected ProcessingStepDescriptor[] processingSteps = EMPTY_STEPS;

	protected Map properties = new HashMap(2);

	//QUESTION: Do we need any description or user readable name

	public ArtifactDescriptor(IArtifactDescriptor base) {
		super();
		key = base.getArtifactKey();
		processingSteps = base.getProcessingSteps();
		// TODO figure out a better way to copy the properties
		setProperty(IArtifactDescriptor.ARTIFACT_MD5, base.getProperty(IArtifactDescriptor.ARTIFACT_MD5));
		setProperty(IArtifactDescriptor.ARTIFACT_SIZE, base.getProperty(IArtifactDescriptor.ARTIFACT_SIZE));
		setProperty(IArtifactDescriptor.DOWNLOAD_MD5, base.getProperty(IArtifactDescriptor.DOWNLOAD_MD5));
		setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, base.getProperty(IArtifactDescriptor.DOWNLOAD_SIZE));
		// TODO this property is hardcoded for the blob store.
		//		setProperty("artifact.uuid", base.getProperty("artifact.uuid"));
	}

	public ArtifactDescriptor(ArtifactDescriptor base) {
		super();
		key = base.key;
		processingSteps = base.processingSteps;
		properties = base.properties;
	}

	public ArtifactDescriptor(IArtifactKey key) {
		super();
		this.key = key;
	}

	public IArtifactKey getArtifactKey() {
		return key;
	}

	public String getProperty(String key) {
		return (String) properties.get(key);
	}

	public void setProperty(String key, String value) {
		if (value == null)
			properties.remove(key);
		else
			properties.put(key, value);
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
}