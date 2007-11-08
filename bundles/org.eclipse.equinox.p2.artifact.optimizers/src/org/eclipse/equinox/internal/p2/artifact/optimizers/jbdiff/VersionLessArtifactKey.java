/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
/**
 * 
 */
package org.eclipse.equinox.internal.p2.artifact.optimizers.jbdiff;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.osgi.framework.Version;

final class VersionLessArtifactKey implements IArtifactKey {
	private String classifier;
	private String id;
	private String namespace;

	public VersionLessArtifactKey(IArtifactKey copyFrom) {
		this.classifier = copyFrom.getClassifier();
		this.id = copyFrom.getId();
		this.namespace = copyFrom.getNamespace();
	}

	public String getClassifier() {
		return classifier;
	}

	public String getId() {
		return id;
	}

	public String getNamespace() {
		return namespace;
	}

	public Version getVersion() {
		return Version.emptyVersion;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof VersionLessArtifactKey))
			return false;
		final VersionLessArtifactKey other = (VersionLessArtifactKey) obj;
		if (classifier == null) {
			if (other.classifier != null)
				return false;
		} else if (!classifier.equals(other.classifier))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (namespace == null) {
			if (other.namespace != null)
				return false;
		} else if (!namespace.equals(other.namespace))
			return false;
		return true;
	}

}