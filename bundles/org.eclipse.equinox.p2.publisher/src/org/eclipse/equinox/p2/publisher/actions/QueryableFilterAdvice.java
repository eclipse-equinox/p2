/*******************************************************************************
 * Copyright (c) 2009, 2017 EclipseSource and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 *   EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.*;

/**
 * An IFilterAdvice that looks up the desired IU in the publisher's input metadata
 * repository and returns whatever filter is found there.
 */
public class QueryableFilterAdvice implements IFilterAdvice {

	private IQueryable<IInstallableUnit> queryable;

	public QueryableFilterAdvice(IQueryable<IInstallableUnit> queryable) {
		this.queryable = queryable;
	}

	@Override
	public IMatchExpression<IInstallableUnit> getFilter(String id, Version version, boolean exact) {
		IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(id, version);
		IQueryResult<IInstallableUnit> result = queryable.query(query, null);
		if (!result.isEmpty())
			return result.iterator().next().getFilter();
		if (exact)
			return null;

		query = QueryUtil.createIUQuery(id);
		result = queryable.query(query, null);
		if (!result.isEmpty())
			return result.iterator().next().getFilter();
		return null;
	}

	@Override
	public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
		return true;
	}

}
