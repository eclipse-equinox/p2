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

import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.MatchQuery;

/**
 * An IArtifactQuery returning matching IArtifactKey objects.
 */
public class ArtifactKeyQuery extends MatchQuery {
	public static final ArtifactKeyQuery ALL_KEYS = new ArtifactKeyQuery();

	private String id;
	private String classifier;
	private VersionRange range;
	private IArtifactKey artifactKey;

	/**
	 * Pass the id and/or version range to match IArtifactKeys against.
	 * Passing null results in matching any id/version
	 * @param id - the IArtifactKey id
	 * @param range - A version range
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

	public boolean isMatch(Object candidate) {
		if (!(candidate instanceof IArtifactKey))
			return false;

		if (artifactKey != null)
			return matchKey((IArtifactKey) candidate);

		IArtifactKey key = (IArtifactKey) candidate;

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
