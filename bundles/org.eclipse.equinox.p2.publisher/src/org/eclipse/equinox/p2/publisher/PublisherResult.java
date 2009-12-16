/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others. All rights reserved. This
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
import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;

public class PublisherResult implements IPublisherResult {

	private static final Collection EMPTY_COLLECTION = new ArrayList(0);
	final Map rootIUs = new HashMap();
	final Map nonRootIUs = new HashMap();

	public void addIU(IInstallableUnit iu, String type) {
		if (type == ROOT)
			addIU(rootIUs, iu.getId(), iu);
		if (type == NON_ROOT)
			addIU(nonRootIUs, iu.getId(), iu);
	}

	public void addIUs(Collection ius, String type) {
		for (Iterator i = ius.iterator(); i.hasNext();)
			addIU((IInstallableUnit) i.next(), type);
	}

	private void addIU(Map map, String id, IInstallableUnit iu) {
		Set ius = (Set) map.get(id);
		if (ius == null) {
			ius = new HashSet(11);
			map.put(id, ius);
		}
		ius.add(iu);
	}

	public IInstallableUnit getIU(String id, Version version, String type) {
		if (type == null || type == ROOT) {
			Collection ius = (Collection) rootIUs.get(id);
			for (Iterator i = ius.iterator(); i.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) i.next();
				if (iu.getVersion().equals(version))
					return iu;
			}
		}
		if (type == null || type == NON_ROOT) {
			Collection ius = (Collection) nonRootIUs.get(id);
			for (Iterator i = ius.iterator(); i.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) i.next();
				if (iu.getVersion().equals(version))
					return iu;
			}
		}
		return null;
	}

	// TODO this method really should not be needed as it just returns the first
	// matching IU non-deterministically.
	public IInstallableUnit getIU(String id, String type) {
		if (type == null || type == ROOT) {
			Collection ius = (Collection) rootIUs.get(id);
			if (ius != null && ius.size() > 0)
				return (IInstallableUnit) ius.iterator().next();
		}
		if (type == null || type == NON_ROOT) {
			Collection ius = (Collection) nonRootIUs.get(id);
			if (ius != null && ius.size() > 0)
				return (IInstallableUnit) ius.iterator().next();
		}
		return null;
	}

	/**
	 * Returns the IUs in this result with the given id.
	 */
	public Collection getIUs(String id, String type) {
		if (type == null) {
			ArrayList result = new ArrayList();
			result.addAll(id == null ? flatten(rootIUs.values()) : getIUs(rootIUs, id));
			result.addAll(id == null ? flatten(nonRootIUs.values()) : getIUs(nonRootIUs, id));
			return result;
		}
		if (type == ROOT)
			return id == null ? flatten(rootIUs.values()) : (Collection) rootIUs.get(id);
		if (type == NON_ROOT)
			return id == null ? flatten(nonRootIUs.values()) : (Collection) nonRootIUs.get(id);
		return null;
	}

	private Collection getIUs(Map ius, String id) {
		Collection result = (Collection) ius.get(id);
		return result == null ? EMPTY_COLLECTION : result;
	}

	protected List flatten(Collection values) {
		ArrayList result = new ArrayList();
		for (Iterator i = values.iterator(); i.hasNext();)
			for (Iterator j = ((HashSet) i.next()).iterator(); j.hasNext();)
				result.add(j.next());
		return result;
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

	class QueryableMap implements IQueryable {
		private Map map;

		public QueryableMap(Map map) {
			this.map = map;
		}

		public IQueryResult query(IQuery query, IProgressMonitor monitor) {
			return query.perform(flatten(this.map.values()).iterator(), new Collector());
		}
	}

	/**
	 * Queries both the root and non root IUs
	 */
	public IQueryResult query(IQuery query, IProgressMonitor monitor) {
		IQueryable nonRootQueryable = new QueryableMap(nonRootIUs);
		IQueryable rootQueryable = new QueryableMap(rootIUs);
		return new CompoundQueryable(new IQueryable[] {nonRootQueryable, rootQueryable}).query(query, monitor);
	}
}
