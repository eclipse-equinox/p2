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
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.osgi.framework.Version;

/** 
 * The concrete type for representing IArtifactKey's.
 * <p>
 * See {link IArtifact for a description of the lifecycle of artifact keys) 
 */
public class ArtifactKey implements IArtifactKey {
	private static final char SEP_CHAR = ',';

	private final String namespace;
	private final String id;
	private final String classifier;
	private final Version version;

	public ArtifactKey(String namespace, String classifier, String id, Version version) {
		super();
		Assert.isNotNull(namespace);
		Assert.isNotNull(classifier);
		Assert.isNotNull(id);
		Assert.isNotNull(version);
		if (namespace.indexOf(SEP_CHAR) != -1)
			throw new IllegalArgumentException("comma not allowed in namespace"); //$NON-NLS-1$
		if (classifier.indexOf(SEP_CHAR) != -1)
			throw new IllegalArgumentException("comma not allowed in classifier"); //$NON-NLS-1$
		if (id.indexOf(SEP_CHAR) != -1)
			throw new IllegalArgumentException("comma not allowed in id"); //$NON-NLS-1$
		this.namespace = namespace;
		this.classifier = classifier;
		this.id = id;
		this.version = version;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getClassifier() {
		return classifier;
	}

	public Version getVersion() {
		return version;
	}

	public int hashCode() {
		int hash = id.hashCode();
		hash = 17 * hash + getVersion().hashCode();
		hash = 17 * hash + namespace.hashCode();
		hash = 17 * hash + classifier.hashCode();
		return hash;
	}

	public String toString() {
		return id + '/' + namespace + '/' + classifier + '/' + getVersion();
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof ArtifactKey))
			return false;
		ArtifactKey ak = (ArtifactKey) obj;
		return ak.id.equals(id) && ak.getVersion().equals(getVersion()) && ak.namespace.equals(namespace) && ak.classifier.equals(classifier);
	}

	public String getId() {
		return id;
	}

}
