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
	public class MapIterator implements Iterator<IInstallableUnit> {
		//iterator over the keys in UIMap
		private Iterator<String> unitIterator;
		//iterator over the Set inside a single value of the IUMap
		private Iterator<IInstallableUnit> currentBucket;

		MapIterator() {
			super();
			unitIterator = units.keySet().iterator();
		}

		public boolean hasNext() {
			return unitIterator.hasNext() || (currentBucket != null && currentBucket.hasNext());
		}

		public IInstallableUnit next() {
			if (currentBucket == null || !currentBucket.hasNext())
				currentBucket = units.get(unitIterator.next()).iterator();
			return currentBucket.next();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Map<String,Set<IInstallableUnit>> mapping IU id to iu's with that id.
	 */
	final Map<String, Set<IInstallableUnit>> units = new HashMap<String, Set<IInstallableUnit>>();

	public void add(IInstallableUnit unit) {
		Set<IInstallableUnit> matching = units.get(unit.getId());
		if (matching == null) {
			matching = new HashSet<IInstallableUnit>(2);
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

	public Iterator<IInstallableUnit> iterator() {
		return new MapIterator();
	}

	public IQueryResult<IInstallableUnit> query(InstallableUnitQuery query) {
		//iterate over the entire map, or just the IU's with the given id
		Iterator<IInstallableUnit> candidates;
		if (query.getId() == null)
			candidates = iterator();
		else {
			Collection<IInstallableUnit> bucket = units.get(query.getId());
			if (bucket == null)
				return Collector.emptyCollector();
			candidates = bucket.iterator();
		}
		return query.perform(candidates);

	}

	public void remove(IInstallableUnit unit) {
		Set<IInstallableUnit> matching = units.get(unit.getId());
		if (matching == null)
			return;
		matching.remove(unit);
		if (matching.isEmpty())
			units.remove(unit.getId());
	}

	public void removeAll(Collection<IInstallableUnit> toRemove) {
		for (IInstallableUnit iu : toRemove)
			remove(iu);
	}
}
