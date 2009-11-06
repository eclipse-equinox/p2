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

package org.eclipse.equinox.internal.provisional.p2.artifact.repository;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.MatchQuery;

/**
 * An IArtifactQuery returning matching IArtifactKey objects.
 */
public class ArtifactKeyQuery extends MatchQuery implements IArtifactQuery {
	private String id;
	private VersionRange range;
	private ArtifactKey artifactKey;

	/**
	 * Pass the id and/or version range to match IArtifactKeys against.
	 * Passing null results in matching any id/version
	 * @param id - the IArtifactKey id
	 * @param range - A version range
	 */
	public ArtifactKeyQuery(String id, VersionRange range) {
		this.id = id;
		this.range = range;
	}

	public ArtifactKeyQuery(IArtifactKey key) {
		this.artifactKey = (key.getClass() == ArtifactKey.class) ? (ArtifactKey) key : new ArtifactKey(key);
	}

	public boolean isMatch(Object candidate) {
		if (!(candidate instanceof IArtifactKey))
			return false;

		if (artifactKey != null)
			return matchKey((IArtifactKey) candidate);

		IArtifactKey key = (IArtifactKey) candidate;
		if (id != null && !key.getId().equals(id))
			return false;

		if (range != null && !range.isIncluded(key.getVersion()))
			return false;

		return true;
	}

	protected boolean matchKey(IArtifactKey candidate) {
		ArtifactKey candidateKey = (candidate.getClass() == ArtifactKey.class) ? (ArtifactKey) candidate : new ArtifactKey(candidate);
		return artifactKey.equals(candidateKey);
	}

	// We are interested in IArtifactKey objects
	public Boolean getAcceptArtifactKeys() {
		return Boolean.TRUE;
	}

	// We are not interested in IArtifactDescriptor objects
	public Boolean getAcceptArtifactDescriptors() {
		return Boolean.FALSE;
	}
}
