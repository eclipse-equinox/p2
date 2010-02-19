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

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.query.ExpressionQuery;

/**
 * An IArtifactQuery returning matching IArtifactKey objects.
 * @since 2.0
 */
public class ArtifactKeyQuery extends ExpressionQuery<IArtifactKey> {
	private static IMatchExpression<IArtifactKey> MATCH_ALL_KEYS = ExpressionUtil.getFactory().matchExpression(ExpressionUtil.TRUE_EXPRESSION);
	private static final IExpression matchKey = ExpressionUtil.parse("this == $0"); //$NON-NLS-1$
	private static final IExpression matchID = ExpressionUtil.parse("id == $0"); //$NON-NLS-1$
	private static final IExpression matchIDClassifierRange = ExpressionUtil.parse("id == $0 && version ~= $2 && (null == $1 || classifier == $1)"); //$NON-NLS-1$

	private static IMatchExpression<IArtifactKey> createMatchExpression(IArtifactKey key) {
		return key == null ? MATCH_ALL_KEYS : ExpressionUtil.getFactory().<IArtifactKey> matchExpression(matchKey, key);
	}

	private static IMatchExpression<IArtifactKey> createMatchExpression(String classifier, String id, VersionRange range) {
		if (range == null) {
			if (classifier == null)
				return id == null ? MATCH_ALL_KEYS : ExpressionUtil.getFactory().<IArtifactKey> matchExpression(matchID, id);
			range = VersionRange.emptyRange;
		}
		return ExpressionUtil.getFactory().<IArtifactKey> matchExpression(matchIDClassifierRange, id, classifier, range);
	}

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

	public ArtifactKeyQuery() {
		super(IArtifactKey.class, MATCH_ALL_KEYS);
	}

	public ArtifactKeyQuery(IArtifactKey key) {
		super(IArtifactKey.class, createMatchExpression(key));
	}
}
