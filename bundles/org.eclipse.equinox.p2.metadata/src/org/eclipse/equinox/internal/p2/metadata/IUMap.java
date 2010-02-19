/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc. - rewrite for smaller memory footprint
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.query.IQueryResult;

/**
 * A map that stores {@link IInstallableUnit} instances in a way that is efficient to query
 */
public class IUMap {
	/**
	 * Iterator over all the {@link IInstallableUnit} instances in the map.
	 */
	public class MapIterator implements Iterator<IInstallableUnit> {
		//iterator over the keys in UIMap
		private final Iterator<Object> unitIterator;
		private IInstallableUnit[] currentBucket;
		private int bucketIndex = 0;
		private IInstallableUnit nextElement = null;

		MapIterator() {
			super();
			unitIterator = units.values().iterator();
		}

		public boolean hasNext() {
			return positionNext();
		}

		public IInstallableUnit next() {
			if (!positionNext())
				throw new NoSuchElementException();

			IInstallableUnit nxt = nextElement;
			nextElement = null;
			return nxt;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		private boolean positionNext() {
			if (nextElement != null)
				return true;

			if (currentBucket != null) {
				nextElement = currentBucket[bucketIndex];
				if (++bucketIndex == currentBucket.length) {
					currentBucket = null;
					bucketIndex = -1;
				}
				return true;
			}

			if (!unitIterator.hasNext())
				return false;

			Object val = unitIterator.next();
			if (val instanceof IInstallableUnit)
				nextElement = (IInstallableUnit) val;
			else {
				currentBucket = (IInstallableUnit[]) val;
				nextElement = currentBucket[0];
				bucketIndex = 1;
			}
			return true;
		}
	}

	/**
	 * Map<String,Object> mapping IU id to either arrays of iu's or a single iu with that id.
	 */
	final Map<String, Object> units = new HashMap<String, Object>();

	public void add(IInstallableUnit unit) {
		String key = unit.getId();
		Object matching = units.get(key);
		if (matching == null) {
			units.put(key, unit);
			return;
		}

		// We already had something at this key position. It must be
		// preserved.
		if (matching.getClass().isArray()) {
			// Entry is an array. Add unique
			IInstallableUnit[] iuArr = (IInstallableUnit[]) matching;
			int idx = iuArr.length;
			while (--idx >= 0)
				if (iuArr[idx].equals(unit))
					// This unit has already been added
					return;

			IInstallableUnit[] iuArrPlus = new IInstallableUnit[iuArr.length + 1];
			System.arraycopy(iuArr, 0, iuArrPlus, 0, iuArr.length);
			iuArrPlus[iuArr.length] = unit;
			units.put(unit.getId(), iuArrPlus);
		} else {
			IInstallableUnit old = (IInstallableUnit) matching;
			if (!old.equals(unit))
				units.put(key, new IInstallableUnit[] {old, unit});
		}
	}

	public void addAll(IInstallableUnit[] toAdd) {
		for (int i = 0; i < toAdd.length; i++)
			add(toAdd[i]);
	}

	public void addAll(Collection<IInstallableUnit> toAdd) {
		for (IInstallableUnit unit : toAdd) {
			add(unit);
		}
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
		else
			candidates = getUnits(query.getId()).iterator();
		return query.perform(candidates);

	}

	public boolean contains(IInstallableUnit unit) {
		return get(unit.getId(), unit.getVersion()) != null;
	}

	/**
	 * Returns a collection of units that has the given <code>id</code>.
	 * @param id The id of the desired units. Must not be <code>null</code>.
	 * @return The units corresponding to the given <code>id</code>.
	 */
	public Collection<IInstallableUnit> getUnits(String id) {
		Object bucket = units.get(id);
		if (bucket == null)
			return CollectionUtils.emptyList();
		return bucket.getClass().isArray() ? CollectionUtils.unmodifiableList((IInstallableUnit[]) bucket) : Collections.<IInstallableUnit> singletonList((IInstallableUnit) bucket);
	}

	public IQueryResult<IInstallableUnit> get(String id) {
		return internalGet(id, null);
	}

	private IQueryResult<IInstallableUnit> internalGet(String id, Version version) {
		// TODO can probably optimize this
		InstallableUnitQuery query = null;
		if (version == null) {
			if (id == null) {
				query = InstallableUnitQuery.ANY;
			} else {
				query = new InstallableUnitQuery(id);
			}
		} else {
			query = new InstallableUnitQuery(id, version);
		}
		return query(query);
	}

	public IInstallableUnit get(String id, Version version) {
		IQueryResult<IInstallableUnit> result = internalGet(id, version);
		return result.isEmpty() ? null : result.iterator().next();
	}

	public void remove(IInstallableUnit unit) {
		String key = unit.getId();
		Object matching = units.get(key);
		if (matching == null)
			return;

		if (matching instanceof IInstallableUnit && matching.equals(unit)) {
			units.remove(key);
			return;
		}

		IInstallableUnit[] array = (IInstallableUnit[]) matching;
		int idx = array.length;
		while (--idx >= 0) {
			if (unit.equals(array[idx])) {
				if (array.length == 2) {
					// We no longer need this array. Replace it with the
					// entry that we keep.
					units.put(key, idx == 0 ? array[1] : array[0]);
					break;
				}

				// Shrink the array
				IInstallableUnit[] newArray = new IInstallableUnit[array.length - 1];
				if (idx > 0)
					System.arraycopy(array, 0, newArray, 0, idx);
				if (idx + 1 < array.length)
					System.arraycopy(array, idx + 1, newArray, idx, array.length - (idx + 1));
				units.put(key, newArray);
				break;
			}
		}
	}

	public void removeAll(Collection<IInstallableUnit> toRemove) {
		for (IInstallableUnit iu : toRemove)
			remove(iu);
	}
}
