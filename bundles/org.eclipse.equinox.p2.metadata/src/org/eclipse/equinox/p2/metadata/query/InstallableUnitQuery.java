/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc. - converted into expression based query
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.query;

import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.*;

/**
 * A query that matches on the id and version of an {@link IInstallableUnit}.
 * @since 2.0
 */
public final class InstallableUnitQuery extends ExpressionQuery<IInstallableUnit> {
	/**
	 * A convenience query that will match any {@link IInstallableUnit}
	 * it encounters.
	 */
	public static final InstallableUnitQuery ANY = new InstallableUnitQuery((String) null);

	private static final IExpression matchID = ExpressionUtil.parse("id == $0"); //$NON-NLS-1$
	private static final IExpression matchIDAndVersion = ExpressionUtil.parse("id == $0 && version == $1"); //$NON-NLS-1$
	private static final IExpression matchIDAndRange = ExpressionUtil.parse("id == $0 && version ~= $1"); //$NON-NLS-1$

	private static IMatchExpression<IInstallableUnit> createMatchExpression(String id) {
		return id == null ? ExpressionQuery.<IInstallableUnit> matchAll() : ExpressionUtil.getFactory().<IInstallableUnit> matchExpression(matchID, id);
	}

	private static IMatchExpression<IInstallableUnit> createMatchExpression(String id, VersionRange range) {
		if (range == null || range.equals(VersionRange.emptyRange))
			return createMatchExpression(id);
		if (range.getMinimum().equals(range.getMaximum()))
			return createMatchExpression(id, range.getMinimum());
		return id == null ? ExpressionQuery.<IInstallableUnit> matchAll() : ExpressionUtil.getFactory().<IInstallableUnit> matchExpression(matchIDAndRange, id, range);
	}

	private static IMatchExpression<IInstallableUnit> createMatchExpression(String id, Version version) {
		return version == null || version.equals(Version.emptyVersion) ? createMatchExpression(id) : ExpressionUtil.getFactory().<IInstallableUnit> matchExpression(matchIDAndVersion, id, version);
	}

	/**
	 * Creates a query that will match any {@link IInstallableUnit} with the given
	 * id, regardless of version.
	 * 
	 * @param id The installable unit id to match, or <code>null</code> to match any id
	 */
	public InstallableUnitQuery(String id) {
		super(IInstallableUnit.class, createMatchExpression(id));
	}

	/**
	 * Creates a query that will match any {@link IInstallableUnit} with the given
	 * id, and whose version falls in the provided range.
	 * 
	 * @param id The installable unit id to match, or <code>null</code> to match any id
	 * @param range The version range to match
	 */
	public InstallableUnitQuery(String id, VersionRange range) {
		super(IInstallableUnit.class, createMatchExpression(id, range));
	}

	/**
	 * Creates a query that will match any {@link IInstallableUnit} with the given
	 * id and version.
	 * 
	 * @param id The installable unit id to match, or <code>null</code> to match any id
	 * @param version The precise version that a matching unit must have
	 */
	public InstallableUnitQuery(String id, Version version) {
		super(IInstallableUnit.class, createMatchExpression(id, version));
	}

	/**
	 * Creates a query that will match any {@link IInstallableUnit} with the given
	 * id and version.
	 * 
	 * @param versionedId The precise id/version combination that a matching unit must have
	 */
	public InstallableUnitQuery(IVersionedId versionedId) {
		this(versionedId.getId(), versionedId.getVersion());
	}

	/**
	 * Returns the id that this query will match against.
	 * @return The installable unit it
	 */
	public String getId() {
		Object[] params = getExpression().getParameters();
		return params.length > 0 ? (String) params[0] : null;
	}
}
