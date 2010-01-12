/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.repository.artifact;

import org.eclipse.equinox.internal.provisional.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.MatchQuery;

/**
 * An IArtifactQuery returning matching IArtifactKey objects.
 * @since 2.0
 */
public class ArtifactKeyQuery extends MatchQuery<IArtifactKey> {
	public static final ArtifactKeyQuery ALL_KEYS = new ArtifactKeyQuery();

	private String id;
	private String classifier;
	private VersionRange range;
	private IArtifactKey artifactKey;

	/**
	 * Pass the id and/or version range to match IArtifactKeys against.
	 * Passing null results in matching any id/version
	 * @param classifier The artifact key classifier, or <code>null</code>
	 * @param id The artifact key id, or <code>null</code>
	 * @param range A version range, or <code>null</code>
	 */
	public ArtifactKeyQuery(String classifier, String id, VersionRange range) {
		this.id = id;
		this.classifier = classifier;
		this.range = range;
	}

	public ArtifactKeyQuery() {
		//matches everything
	}

	public ArtifactKeyQuery(IArtifactKey key) {
		this.artifactKey = key;
	}

	public boolean isMatch(IArtifactKey key) {
		if (artifactKey != null)
			return matchKey(key);

		if (classifier != null && !key.getClassifier().equals(classifier))
			return false;

		if (id != null && !key.getId().equals(id))
			return false;

		if (range != null && !range.isIncluded(key.getVersion()))
			return false;

		return true;
	}

	protected boolean matchKey(IArtifactKey candidate) {
		return artifactKey.equals(candidate);
	}

	// We are interested in IArtifactKey objects
	public Boolean getExcludeArtifactKeys() {
		return Boolean.FALSE;
	}

	// We are not interested in IArtifactDescriptor objects
	public Boolean getExcludeArtifactDescriptors() {
		return Boolean.TRUE;
	}
}
