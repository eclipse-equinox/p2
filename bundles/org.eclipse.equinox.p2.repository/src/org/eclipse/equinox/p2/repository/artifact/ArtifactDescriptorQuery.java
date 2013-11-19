/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc. - converted into expression based query
 *******************************************************************************/

package org.eclipse.equinox.p2.repository.artifact;

import java.util.*;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.query.ExpressionMatchQuery;

/**
 * A general purpose query for matching {@link IArtifactDescriptor} instances
 * that satisfy various criteria.
 * 
 * @since 2.0
 */
public final class ArtifactDescriptorQuery extends ExpressionMatchQuery<IArtifactDescriptor> {

	private static final IExpression descriptorMatch = ExpressionUtil.parse(//
			"artifactKey.id == $0 && artifactKey.version ~= $1 && ($2.empty || $2.all(x | properties[x.key] == x.value))"); //$NON-NLS-1$

	private static IMatchExpression<IArtifactDescriptor> createExpression(String id, VersionRange range, String format, Map<String, String> properties) {
		if (range == null)
			range = VersionRange.emptyRange;
		if (format != null) {
			if (properties == null || properties.isEmpty())
				properties = Collections.singletonMap(IArtifactDescriptor.FORMAT, format);
			else {
				properties = new HashMap<String, String>(properties);
				properties.put(IArtifactDescriptor.FORMAT, format);
			}
		} else if (properties == null)
			properties = Collections.<String, String> emptyMap();

		IExpressionFactory factory = ExpressionUtil.getFactory();
		return factory.<IArtifactDescriptor> matchExpression(descriptorMatch, id, range, properties);
	}

	/**
	 * A singleton query that will match all instances of {@link IArtifactDescriptor}.
	 */
	public static final ArtifactDescriptorQuery ALL_DESCRIPTORS = new ArtifactDescriptorQuery();

	/**
	 * Clients must use {@link #ALL_DESCRIPTORS}.
	 */
	private ArtifactDescriptorQuery() {
		//matches everything
		super(IArtifactDescriptor.class, ExpressionUtil.TRUE_EXPRESSION);
	}

	/**
	 * The query will match descriptors with the given <code>id</code>, <code>versionRange</code>
	 * and <code>format</code>
	 * @param id the descriptor id to match. Can not be <code>null</code>
	 * @param versionRange the descriptor version range to match or <code>null</code> to match
	 * any version range
	 * @param format the descriptor {@link IArtifactDescriptor#FORMAT} value to match, or <code>null</code> to
	 * match any descriptor format
	 */
	public ArtifactDescriptorQuery(String id, VersionRange versionRange, String format) {
		super(IArtifactDescriptor.class, createExpression(id, versionRange, format, null));
	}

	/**
	 * The query will match descriptors whose <code>id</code> and <code>versionRange</code>
	 * match the supplied key
	 * @param key the artifact key to match.  Cannot be <code>null</code>.
	 */
	public ArtifactDescriptorQuery(IArtifactKey key) {
		super(IArtifactDescriptor.class, createExpression(key.getId(), new VersionRange(key.getVersion(), true, key.getVersion(), true), null, null));
	}

	/**
	 * The query will match descriptors with the given <code>id</code>, <code>versionRange</code>,
	 * <code>format</code>, and <code>properties</code>.
	 * @param id the descriptor id to match. Can not be <code>null</code>
	 * @param versionRange the descriptor version range to match or <code>null</code> to match
	 * any version range
	 * @param format the descriptor {@link IArtifactDescriptor#FORMAT} value to match, or <code>null</code> to
	 * match any descriptor format
	 * @param properties The properties to query for
	 */
	public ArtifactDescriptorQuery(String id, VersionRange versionRange, String format, Map<String, String> properties) {
		super(IArtifactDescriptor.class, createExpression(id, versionRange, format, properties));
	}
}
