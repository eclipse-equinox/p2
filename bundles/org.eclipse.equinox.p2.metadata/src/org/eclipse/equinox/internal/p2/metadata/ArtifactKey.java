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
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.Objects;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IMemberProvider;

/**
 * The concrete type for representing IArtifactKey's.
 * <p>
 * See {link IArtifact for a description of the lifecycle of artifact keys)
 */
public class ArtifactKey implements IArtifactKey, IMemberProvider {
	private static final String SEPARATOR = ","; //$NON-NLS-1$

	public static final String MEMBER_ID = "id"; //$NON-NLS-1$
	public static final String MEMBER_CLASSIFIER = "classifier"; //$NON-NLS-1$
	public static final String MEMBER_VERSION = "version"; //$NON-NLS-1$

	private final String id;
	private final String classifier;
	private final Version version;

	public static IArtifactKey parse(String specification) {
		String[] parts = specification.split(SEPARATOR, -1);
		if (parts.length < 2 || parts.length > 3) {
			throw new IllegalArgumentException("Unexpected number of parts in artifact key: " + specification); //$NON-NLS-1$
		}
		Version version = Version.emptyVersion;
		if (parts.length == 3 && !parts[2].isBlank()) {
			version = Version.parseVersion(parts[2]);
		}
		try {
			return new ArtifactKey(parts[0], parts[1], version);
		} catch (IllegalArgumentException e) {
			throw (IllegalArgumentException) new IllegalArgumentException(
					"Wrong version syntax in artifact key: " + specification).initCause(e); //$NON-NLS-1$
		}
	}

	public ArtifactKey(String classifier, String id, Version version) {
		Assert.isNotNull(classifier);
		Assert.isNotNull(id);
		Assert.isNotNull(version);
		if (classifier.contains(SEPARATOR)) {
			throw new IllegalArgumentException("comma not allowed in classifier"); //$NON-NLS-1$
		}
		if (id.contains(SEPARATOR)) {
			throw new IllegalArgumentException("comma not allowed in id"); //$NON-NLS-1$
		}
		this.classifier = classifier;
		this.id = id;
		this.version = version;
	}

	public ArtifactKey(IArtifactKey artifactKey) {
		this.classifier = artifactKey.getClassifier();
		this.id = artifactKey.getId();
		this.version = artifactKey.getVersion();
	}

	@Override
	public String getClassifier() {
		return classifier;
	}

	@Override
	public Version getVersion() {
		return version;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, getVersion(), classifier);
	}

	@Override
	public String toString() {
		return classifier + SEPARATOR + id + SEPARATOR + getVersion();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return obj instanceof IArtifactKey ak //
				&& ak.getId().equals(id) && ak.getVersion().equals(getVersion())
				&& ak.getClassifier().equals(classifier);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String toExternalForm() {
		return String.join(SEPARATOR, classifier, id, version.toString());
	}

	@Override
	public Object getMember(String memberName) {
		return switch (memberName) {
		case MEMBER_ID -> id;
		case MEMBER_VERSION -> version;
		case MEMBER_CLASSIFIER -> classifier;
		default -> throw new IllegalArgumentException("No such member: " + memberName); //$NON-NLS-1$
		};
	}
}
