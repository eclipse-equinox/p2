/*******************************************************************************
 * Copyright (c) 2009 EclipseSource and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.actions;

import org.eclipse.equinox.internal.p2.metadata.LDAPQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;

/**
 * An IFilterAdvice that looks up the desired IU in the publisher's input metadata
 * repository and returns whatever filter is found there.
 */
public class QueryableFilterAdvice implements IFilterAdvice {

	private IQueryable<IInstallableUnit> queryable;

	public QueryableFilterAdvice(IQueryable<IInstallableUnit> queryable) {
		this.queryable = queryable;
	}

	public String getFilter(String id, Version version, boolean exact) {
		InstallableUnitQuery query = new InstallableUnitQuery(id, version);
		IQueryResult<IInstallableUnit> result = queryable.query(query, null);
		if (!result.isEmpty())
			return ((LDAPQuery) result.iterator().next().getFilter()).getFilter();
		if (exact)
			return null;

		query = new InstallableUnitQuery(id);
		result = queryable.query(query, null);
		if (!result.isEmpty())
			return ((LDAPQuery) result.iterator().next().getFilter()).getFilter();
		return null;
	}

	public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
		return true;
	}

}
