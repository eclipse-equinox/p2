/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.IUMap;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.query.*;

public class PublisherResult implements IPublisherResult {

	final IUMap rootIUs = new IUMap();
	final IUMap nonRootIUs = new IUMap();

	public void addIU(IInstallableUnit iu, String type) {
		if (type == ROOT)
			rootIUs.add(iu);
		if (type == NON_ROOT)
			nonRootIUs.add(iu);
	}

	public void addIUs(Collection<IInstallableUnit> ius, String type) {
		for (IInstallableUnit iu : ius)
			addIU(iu, type);
	}

	public IInstallableUnit getIU(String id, Version version, String type) {
		if (type == null || type == ROOT) {
			IInstallableUnit result = rootIUs.get(id, version);
			if (result != null)
				return result;
		}
		if (type == null || type == NON_ROOT) {
			IInstallableUnit result = nonRootIUs.get(id, version);
			if (result != null)
				return result;
		}
		return null;
	}

	// TODO this method really should not be needed as it just returns the first
	// matching IU non-deterministically.
	public IInstallableUnit getIU(String id, String type) {
		if (type == null || type == ROOT) {
			IQueryResult<IInstallableUnit> ius = rootIUs.get(id);
			if (!ius.isEmpty())
				return ius.iterator().next();
		}
		if (type == null || type == NON_ROOT) {
			IQueryResult<IInstallableUnit> ius = nonRootIUs.get(id);
			if (!ius.isEmpty())
				return ius.iterator().next();
		}
		return null;
	}

	/**
	 * Returns the IUs in this result with the given id.
	 */
	public Collection<IInstallableUnit> getIUs(String id, String type) {
		if (type == null) {
			// TODO can this be optimized?
			ArrayList<IInstallableUnit> result = new ArrayList<IInstallableUnit>();
			result.addAll(rootIUs.get(id).toSet());
			result.addAll(nonRootIUs.get(id).toSet());
			return result;
		}
		if (type == ROOT)
			return rootIUs.get(id).toSet();
		if (type == NON_ROOT)
			return nonRootIUs.get(id).toSet();
		return null;
	}

	public void merge(IPublisherResult result, int mode) {
		if (mode == MERGE_MATCHING) {
			addIUs(result.getIUs(null, ROOT), ROOT);
			addIUs(result.getIUs(null, NON_ROOT), NON_ROOT);
		} else if (mode == MERGE_ALL_ROOT) {
			addIUs(result.getIUs(null, ROOT), ROOT);
			addIUs(result.getIUs(null, NON_ROOT), ROOT);
		} else if (mode == MERGE_ALL_NON_ROOT) {
			addIUs(result.getIUs(null, ROOT), NON_ROOT);
			addIUs(result.getIUs(null, NON_ROOT), NON_ROOT);
		}
	}

	/**
	 * Queries both the root and non root IUs
	 */
	public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		//optimize for installable unit query
		if (query instanceof InstallableUnitQuery) {
			return queryIU((InstallableUnitQuery) query, monitor);
		} else if (query instanceof LimitQuery<?>) {
			return doLimitQuery((LimitQuery<IInstallableUnit>) query, monitor);
		} else if (query instanceof PipedQuery<?>) {
			return doPipedQuery((PipedQuery<IInstallableUnit>) query, monitor);
		}
		IQueryResult<IInstallableUnit> nonRootQueryable = nonRootIUs.query(InstallableUnitQuery.ANY);
		IQueryResult<IInstallableUnit> rootQueryable = rootIUs.query(InstallableUnitQuery.ANY);
		return new CompoundQueryable<IInstallableUnit>(nonRootQueryable, rootQueryable).query(query, monitor);
	}

	/**
	 * Optimize performance of LimitQuery for cases where we know how to optimize
	 * the child query.
	 */
	private IQueryResult<IInstallableUnit> doLimitQuery(LimitQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		//perform the child query first so it can be optimized
		IQuery<IInstallableUnit> child = query.getQueries().get(0);
		return query(child, monitor).query(query, monitor);
	}

	/**
	 * Optimize performance of PipedQuery for cases where we know how to optimize
	 * the child query.
	 */
	private IQueryResult<IInstallableUnit> doPipedQuery(PipedQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		IQueryResult<IInstallableUnit> last = Collector.emptyCollector();
		List<IQuery<IInstallableUnit>> queries = query.getQueries();
		if (!queries.isEmpty()) {
			//call our method to do optimized execution of first query
			last = query(queries.get(0), monitor);
			for (int i = 1; i < queries.size(); i++) {
				if (last.isEmpty())
					break;
				//Can't optimize the rest, but likely at this point the result set is much smaller
				last = queries.get(i).perform(last.iterator());
			}
		}
		return last;
	}

	private IQueryResult<IInstallableUnit> queryIU(InstallableUnitQuery query, IProgressMonitor monitor) {
		IQueryResult<IInstallableUnit> rootResult = rootIUs.query(query);
		IQueryResult<IInstallableUnit> nonRootResult = nonRootIUs.query(query);
		Collector<IInstallableUnit> result = new Collector<IInstallableUnit>();
		result.addAll(rootResult);
		result.addAll(nonRootResult);
		return result;
	}
}
