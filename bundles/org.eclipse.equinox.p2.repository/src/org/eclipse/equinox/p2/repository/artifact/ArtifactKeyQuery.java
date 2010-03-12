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

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.query.ExpressionMatchQuery;
import org.eclipse.equinox.p2.query.IQueryable;

/**
 * A general purpose query for matching {@link IArtifactKey} instances
 * that satisfy various criteria.
 * 
 * @since 2.0
 */
public final class ArtifactKeyQuery extends ExpressionMatchQuery<IArtifactKey> {
	private static final IExpression matchKey = ExpressionUtil.parse("this == $0"); //$NON-NLS-1$
	private static final IExpression matchID = ExpressionUtil.parse("id == $0"); //$NON-NLS-1$
	private static final IExpression matchIDClassifierRange = ExpressionUtil.parse("id == $0 && version ~= $2 && (null == $1 || classifier == $1)"); //$NON-NLS-1$

	private static IExpression createMatchExpression(IArtifactKey key) {
		if (key == null)
			return ExpressionUtil.TRUE_EXPRESSION;
		return ExpressionUtil.getFactory().<IArtifactKey> matchExpression(matchKey, key);
	}

	private static IExpression createMatchExpression(String classifier, String id, VersionRange range) {
		if (range == null) {
			if (classifier == null) {
				if (id == null)
					return ExpressionUtil.TRUE_EXPRESSION;
				return ExpressionUtil.getFactory().<IArtifactKey> matchExpression(matchID, id);
			}
			range = VersionRange.emptyRange;
		}
		return ExpressionUtil.getFactory().<IArtifactKey> matchExpression(matchIDClassifierRange, id, classifier, range);
	}

	/**
	 * A singleton artifact key query that will always match every artifact key in
	 * the given {@link IQueryable}.
	 */
	public static final ArtifactKeyQuery ALL_KEYS = new ArtifactKeyQuery();

	/**
	 * Pass the id and/or version range to match IArtifactKeys against.
	 * Passing null results in matching any id/version
	 * @param classifier The artifact key classifier, or <code>null</code>
	 * @param id The artifact key id, or <code>null</code>
	 * @param range A version range, or <code>null</code>
	 */
	public ArtifactKeyQuery(String classifier, String id, VersionRange range) {
		super(IArtifactKey.class, createMatchExpression(classifier, id, range));
	}

	private ArtifactKeyQuery() {
		super(IArtifactKey.class, ExpressionUtil.TRUE_EXPRESSION);
	}

	/**
	 * Creates an artifact key query that will match any key equal to the
	 * provided key
	 * @param key the input key to test for equality in the query
	 */
	public ArtifactKeyQuery(IArtifactKey key) {
		super(IArtifactKey.class, createMatchExpression(key));
	}
}
