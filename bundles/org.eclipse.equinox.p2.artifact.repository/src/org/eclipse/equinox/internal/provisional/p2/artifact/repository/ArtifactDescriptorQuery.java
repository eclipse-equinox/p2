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

import org.eclipse.equinox.internal.provisional.p2.metadata.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.MatchQuery;

/**
 * An implementation of IArtifactQuery that matches IArtifactDescriptors
 */
public class ArtifactDescriptorQuery extends MatchQuery implements IArtifactQuery {
	private VersionRange range = null;
	private String id = null;
	private String format = null;
	private ArtifactDescriptor descriptor = null;
	private IArtifactRepository repository = null;

	/**
	 * The query will match descriptors with the given id, version and format
	 * If any parameter is null, that attribute will be ignored
	 * @param id - the id to match, or null
	 * @param versionRange - the version range to match or null
	 * @param format - {@link IArtifactDescriptor#FORMAT} value to match, or null
	 */
	public ArtifactDescriptorQuery(String id, VersionRange versionRange, String format) {
		this(id, versionRange, format, null);
	}

	/**
	 * The query will match descriptors with the given id, version range, format and repository
	 * if any parameter is null, that attribute will be ignored
	 * @param id - the id to match, or null
	 * @param versionRange - the version range to match or null
	 * @param format - {@link IArtifactDescriptor#FORMAT} value to match, or null
	 * @param repository
	 */
	public ArtifactDescriptorQuery(String id, VersionRange versionRange, String format, IArtifactRepository repository) {
		this.id = id;
		this.range = versionRange;
		this.format = format;
		this.repository = repository;
	}

	/**
	 * The query will match candidate descriptors where
	 *          new ArtifactDescriptor(descriptor).equals(new ArtifactDescriptor(candidate))
	 * @param descriptor
	 */
	public ArtifactDescriptorQuery(IArtifactDescriptor descriptor) {
		this.descriptor = (descriptor.getClass() == ArtifactDescriptor.class) ? (ArtifactDescriptor) descriptor : new ArtifactDescriptor(descriptor);
	}

	public boolean isMatch(Object candidate) {
		if (!(candidate instanceof IArtifactDescriptor))
			return false;

		if (descriptor != null)
			return matchDescriptor((IArtifactDescriptor) candidate);

		IArtifactDescriptor candidateDescriptor = (IArtifactDescriptor) candidate;
		if (id != null && !id.equals(candidateDescriptor.getArtifactKey().getId()))
			return false;

		if (range != null && !range.isIncluded(candidateDescriptor.getArtifactKey().getVersion()))
			return false;

		if (format != null && !format.equals(candidateDescriptor.getProperty(IArtifactDescriptor.FORMAT)))
			return false;

		if (repository != null && repository != candidateDescriptor.getRepository())
			return false;

		return true;
	}

	protected boolean matchDescriptor(IArtifactDescriptor candidate) {
		ArtifactDescriptor candidateDescriptor = (candidate.getClass() == ArtifactDescriptor.class) ? (ArtifactDescriptor) candidate : new ArtifactDescriptor(candidate);
		return descriptor.equals(candidateDescriptor);
	}

	public Boolean getAcceptArtifactDescriptors() {
		return Boolean.TRUE;
	}

	public Boolean getAcceptArtifactKeys() {
		return Boolean.FALSE;
	}
}
