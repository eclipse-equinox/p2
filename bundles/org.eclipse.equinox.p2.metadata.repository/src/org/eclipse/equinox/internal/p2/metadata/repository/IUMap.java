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
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;

/**
 * A map that stores {@link IInstallableUnit} instances in a way that is efficient to query
 */
public class IUMap {
	/**
	 * Iterator over all the {@link IInstallableUnit} instances in the map.
	 */
	public class MapIterator implements Iterator {
		//iterator over the keys in UIMap
		private Iterator unitIterator;
		//iterator over the Set inside a single value of the IUMap
		private Iterator currentBucket;

		MapIterator() {
			super();
			unitIterator = units.keySet().iterator();
		}

		public boolean hasNext() {
			return unitIterator.hasNext() || (currentBucket != null && currentBucket.hasNext());
		}

		public Object next() {
			if (currentBucket == null || !currentBucket.hasNext())
				currentBucket = ((Set) units.get(unitIterator.next())).iterator();
			return currentBucket.next();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Map<String,Set<IInstallableUnit>> mapping IU id to iu's with that id.
	 */
	final Map units = new HashMap();

	public void add(IInstallableUnit unit) {
		Set matching = (Set) units.get(unit.getId());
		if (matching == null) {
			matching = new HashSet(2);
			units.put(unit.getId(), matching);
		}
		matching.add(unit);
	}

	public void addAll(IInstallableUnit[] toAdd) {
		for (int i = 0; i < toAdd.length; i++)
			add(toAdd[i]);
	}

	public void clear() {
		units.clear();
	}

	public Iterator iterator() {
		return new MapIterator();
	}

	public IQueryResult query(InstallableUnitQuery query) {
		//iterate over the entire map, or just the IU's with the given id
		Iterator candidates;
		if (query.getId() == null)
			candidates = iterator();
		else {
			Collection bucket = ((Collection) units.get(query.getId()));
			if (bucket == null)
				return Collector.EMPTY_COLLECTOR;
			candidates = bucket.iterator();
		}
		return query.perform(candidates);

	}

	public void remove(IInstallableUnit unit) {
		Set matching = (Set) units.get(unit.getId());
		if (matching == null)
			return;
		matching.remove(unit);
		if (matching.isEmpty())
			units.remove(unit.getId());
	}

	public void removeAll(Collection toRemove) {
		for (Iterator it = toRemove.iterator(); it.hasNext();)
			remove((IInstallableUnit) it.next());
	}
}
