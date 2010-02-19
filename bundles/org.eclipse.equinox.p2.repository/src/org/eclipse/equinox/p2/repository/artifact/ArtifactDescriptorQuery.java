/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.repository.artifact;

import java.util.Iterator;
import java.util.Properties;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.MatchQuery;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;

/**
 * A general purpose query for matching {@link IArtifactDescriptor} instances
 * that satisfy various criteria.
 * 
 * @since 2.0
 */
public class ArtifactDescriptorQuery extends MatchQuery<IArtifactDescriptor> {

	/**
	 * A singleton query that will match all instances of {@link IArtifactDescriptor}.
	 */
	public static final ArtifactDescriptorQuery ALL_DESCRIPTORS = new ArtifactDescriptorQuery();

	private ArtifactDescriptor descriptor = null;
	private String format = null;
	private String id = null;
	private Properties properties = null;
	private VersionRange range = null;
	private IArtifactRepository repository = null;

	/**
	 * Clients must use {@link #ALL_DESCRIPTORS}.
	 */
	private ArtifactDescriptorQuery() {
		//matches everything
	}

	/**
	 * The query will match candidate descriptors where:
	 * <pre>
	 *     new ArtifactDescriptor(descriptor).equals(new ArtifactDescriptor(candidate))
	 * </pre>
	 * @param descriptor The descriptor to match
	 */
	public ArtifactDescriptorQuery(IArtifactDescriptor descriptor) {
		this.descriptor = (descriptor.getClass() == ArtifactDescriptor.class) ? (ArtifactDescriptor) descriptor : new ArtifactDescriptor(descriptor);
	}

	/**
	 * The query will match descriptors with the given id, version and format
	 * If any parameter is null, that attribute will be ignored.
	 * 
	 * @param id the descriptor id to match, or <code>null</code> to match any id
	 * @param versionRange the descriptor version range to match or <code>null</code> to match
	 * any version range
	 * @param format the descriptor {@link IArtifactDescriptor#FORMAT} value to match, or <code>null</code> to
	 * match any descriptor format
	 */
	public ArtifactDescriptorQuery(String id, VersionRange versionRange, String format) {
		this(id, versionRange, format, null);
	}

	/**
	 * The query will match descriptors with the given id, version range, format and repository
	 * if any parameter is null, that attribute will be ignored.
	 * 
	 * @param id the descriptor id to match, or <code>null</code> to match any id
	 * @param versionRange the descriptor version range to match or <code>null</code> to match
	 * any version range
	 * @param format the descriptor {@link IArtifactDescriptor#FORMAT} value to match, or <code>null</code> to
	 * match any descriptor format
	 * @param repository The repository of the descriptor to match, or <code>null</code>
	 * to match descriptors from any repository
	 */
	public ArtifactDescriptorQuery(String id, VersionRange versionRange, String format, IArtifactRepository repository) {
		this.id = id;
		this.range = versionRange;
		this.format = format;
		this.repository = repository;
	}

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.p2.query.MatchQuery#isMatch(java.lang.Object)
	 */
	public boolean isMatch(IArtifactDescriptor candidate) {
		if (descriptor != null)
			return matchDescriptor(candidate);

		if (id != null && !id.equals(candidate.getArtifactKey().getId()))
			return false;

		if (range != null && !range.isIncluded(candidate.getArtifactKey().getVersion()))
			return false;

		if (format != null && !format.equals(candidate.getProperty(IArtifactDescriptor.FORMAT)))
			return false;

		if (repository != null && repository != candidate.getRepository())
			return false;

		if (properties != null) {
			for (Iterator<Object> iterator = properties.keySet().iterator(); iterator.hasNext();) {
				String key = (String) iterator.next();
				String value = properties.getProperty(key);
				if (value != null && !value.equals(candidate.getProperty(key)))
					return false;
			}
		}
		return true;
	}

	private boolean matchDescriptor(IArtifactDescriptor candidate) {
		ArtifactDescriptor candidateDescriptor = (candidate.getClass() == ArtifactDescriptor.class) ? (ArtifactDescriptor) candidate : new ArtifactDescriptor(candidate);
		return descriptor.equals(candidateDescriptor);
	}

	/**
	 * Sets the properties that this query should match against. This query will only match
	 * descriptors that have property keys and values matching those provided here.
	 * @param properties The properties to query for
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
}
