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

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.osgi.framework.Version;

/** 
 * The concrete type for representing IArtifactKey's.
 * <p>
 * See {link IArtifact for a description of the lifecycle of artifact keys) 
 */
public class ArtifactKey implements IArtifactKey {

	static final int NO_SEGMENTS = 5;
	private static final char SEP_CHAR = ',';

	private String namespace;
	private String id;
	private String classifier;
	private transient Version versionObject;
	private String version;

	public ArtifactKey(String namespace, String classifier, String id, Version aVersion) {
		super();
		if (namespace.indexOf(SEP_CHAR) != -1)
			throw new IllegalArgumentException("comma not allowed in namespace"); //$NON-NLS-1$
		if (classifier.indexOf(SEP_CHAR) != -1)
			throw new IllegalArgumentException("comma not allowed in classifier"); //$NON-NLS-1$
		if (id.indexOf(SEP_CHAR) != -1)
			throw new IllegalArgumentException("comma not allowed in id"); //$NON-NLS-1$
		this.namespace = namespace;
		this.classifier = classifier;
		this.id = id;
		this.versionObject = aVersion;
		this.version = aVersion == null ? null : aVersion.toString();
	}

	public String getNamespace() {
		return namespace;
	}

	public String getClassifier() {
		return classifier;
	}

	public Version getVersion() {
		if (versionObject == null)
			versionObject = version == null ? Version.emptyVersion : new Version(version);
		return versionObject;
	}

	public int hashCode() {
		int hash = id.hashCode();
		hash = 17 * hash + getVersion().hashCode();
		hash = 17 * hash + namespace.hashCode();
		hash = 17 * hash + classifier.hashCode();
		return hash;
	}

	public String toString() {
		return "ArtifactKey=" + id + '/' + namespace + '/' + classifier + '/' + getVersion(); //$NON-NLS-1$
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
