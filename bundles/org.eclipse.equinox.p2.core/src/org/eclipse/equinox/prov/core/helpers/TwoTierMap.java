/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.prov.core.helpers;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.*;

/**
 * The purpose of this class is to provide a two-tier map.
 * MapOf(key1 => MapOf(key2 => value)).
 * Even though this class implements Map the behavior of
 * the methods aren't exactly the same as that of a real
 * Map - especially, entrySet(), keySet() etc. works off
 * the outer map while values() returns all the values of
 * all the inner maps.
 */
public class TwoTierMap implements Map, Serializable {

	private static final long serialVersionUID = 362497720873186265L;

	private Map outerMap;
	private int policy;

	public static final int POLICY_NONE = 0, POLICY_BOTH_MAPS_PRESERVE_ORDERING = 1 << 0, POLICY_INNER_MAP_PRESERVE_EXISTING = 1 << 1, POLICY_INNER_MAP_SORTED_ASCENDING = 1 << 2, POLICY_INNER_MAP_SORTED_DESCENDING = 1 << 3, POLICY_INNER_MAP_ENSURE_SINGLETON = 1 << 4;

	private static final int POLICY_INNER_MAP_SORTED_MASK = POLICY_INNER_MAP_SORTED_ASCENDING | POLICY_INNER_MAP_SORTED_DESCENDING;

	public TwoTierMap() {
		this(8, POLICY_NONE);
	}

	public TwoTierMap(int initialCapacity) {
		this(initialCapacity, POLICY_NONE);
	}

	/**
	 * Creates a two-tier map with the specified
	 * initialCapacity and policy. The policy determines
	 * whether the outer map is ordered, inner map is
	 * sorted, clobber values of inner map etc.
	 */
	public TwoTierMap(int initialCapacity, int policy) {
		this.policy = policy;
		this.outerMap = shouldUseOrderedMap() ? new LinkedHashMap(initialCapacity) : new HashMap(initialCapacity);
	}

	/**
	 * Insert the value with key key1 into the inner map 
	 * that is obtained from the outer map with key key2.
	 * If you have set POLICY_INNER_MAP_PRESERVE_EXISTING
	 * at the time of creating this, it will not overwrite
	 * if there is already a non-null value at key2. 
	 * The other POLICY_INNER_MAP_* policies determine 
	 * what kind of inner map is created.
	 * @param key1 The key for outer map.
	 * @param key2 The key for inner map.
	 * @param value The value.
	 * @return Existing value if any, otherwise null.
	 */
	public Object put(Object key1, Object key2, Object value) {
		Map innerMap = (Map) this.outerMap.get(key1);
		if (innerMap == null) {
			if (shouldUseSingletonInnerMap()) {
				this.outerMap.put(key1, Collections.singletonMap(key2, value));
				return null;
			}
			innerMap = createInnerMap();
			this.outerMap.put(key1, innerMap);
		}
		// It is faster to check for already existing entry 
		// this way instead of containsKey() check. Of course,
		// this will prevent us from recognizing a null entry, 
		// which I think shouldn't be a problem.
		Object existing = innerMap.put(key2, value);
		if (existing != null && shouldPreserveExisting()) {
			innerMap.put(key2, existing);
		}
		return existing;
	}

	/**
	 * Get the object stored in the inner map using key2
	 * as key where the inner map is obtained from the 
	 * outer map using key1.
	 * @param key1 The key for outer map.
	 * @param key2 The key for inner map.
	 * @return The object for key2 in inner map for key1 
	 * in the outer map. 
	 */
	public Object get(Object key1, Object key2) {
		if (key1 == null || key2 == null)
			return getAll(key1);
		Map innerMap = (Map) this.outerMap.get(key1);
		Object value = innerMap == null ? null : innerMap.get(key2);
		return value;
	}

	/**
	 * Get all the values in the inner map for key1 in
	 * the outer map.
	 * @param key1 The key for outer map.
	 * @return Collection of values in the inner map.
	 */
	public Collection getAll(Object key1) {
		if (key1 == null)
			// return all
			return values();
		Map innerMap = (Map) this.outerMap.get(key1);
		return innerMap == null ? Collections.EMPTY_LIST : Collections.unmodifiableCollection(innerMap.values());

	}

	public Object remove(Object key1, Object key2) {
		if (key1 == null || key2 == null)
			return removeAll(key1);
		Map innerMap = (Map) this.outerMap.get(key1);
		if (innerMap == null)
			return null;
		if (shouldUseSingletonInnerMap()) {
			Object result = innerMap.get(key2);
			if (result != null) {
				this.outerMap.remove(key1);
			}
			return result;
		}
		Object result = (innerMap == null) ? null : innerMap.remove(key2);
		if (result != null && innerMap.isEmpty()) {
			this.outerMap.remove(key1);
		}
		return result;
	}

	public Collection removeAll(Object key1) {
		if (key1 == null)
			return Collections.EMPTY_LIST;
		Map innerMap = (Map) this.outerMap.remove(key1);
		return innerMap == null ? Collections.EMPTY_LIST : innerMap.values();
	}

	/**
	 * Determine whether there exists a valid object for
	 * key2 in the inner map for key1 in the outer map.
	 * @param key1 The key for outer map.
	 * @param key2 The key for inner map.
	 * @return true if a non-null object exists; otherwise
	 * false.
	 */
	public boolean containsKey(Object key1, Object key2) {
		if (key1 == null)
			return false;
		if (key2 == null)
			return containsKey(key1);
		return get(key1, key2) != null;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		this.outerMap.clear();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		return this.outerMap.containsKey(key);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		for (Iterator it = entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			Map innerMap = (Map) entry.getValue();
			if (innerMap.containsValue(value))
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	public int size() {
		return this.outerMap.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		return this.size() == 0;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet() {
		return Collections.unmodifiableSet(this.outerMap.entrySet());
	}

	/* (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	public Collection values() {
		ArrayList result = new ArrayList(size());
		for (Iterator it = this.outerMap.keySet().iterator(); it.hasNext();) {
			Object key = it.next();
			// A null key will cause infinite recursion!
			if (key != null) {
				result.addAll(getAll(key));
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public Object get(Object key) {
		if (key instanceof Object[]) {
			Object[] keys = (Object[]) key;
			return get(keys[0], keys[1]);
		} else
			return getAll(key);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	public Set keySet() {
		return Collections.unmodifiableSet(this.outerMap.keySet());
	}

	/* (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public Object put(Object key, Object value) {
		if (key instanceof Object[]) {
			Object[] keys = (Object[]) key;
			return put(keys[0], keys[1], value);
		}
		throw new IllegalArgumentException("First arg should be an array!"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map t) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public Object remove(Object key) {
		if (key instanceof Object[]) {
			Object[] keys = (Object[]) key;
			return remove(keys[0], keys[1]);
		} else
			return removeAll(key);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (this.outerMap.isEmpty()) {
			sb.append("  (Empty)"); //$NON-NLS-1$
		} else {
			for (Iterator it = this.outerMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				sb.append("  ").append(entry.getKey()) //$NON-NLS-1$
						.append(" = ") //$NON-NLS-1$
						.append(entry.getValue()).append('\n');
			}
			sb.setLength(sb.length() - 1);
		}
		return sb.toString();
	}

	public void dump(PrintStream ps) {
		if (ps == null) {
			ps = System.out;
		}
		ps.println(this.toString());
	}

	private Map createInnerMap() {
		Map innerMap;
		if (shouldUseSortedInnerMap()) {
			innerMap = new TreeMap(new ValueComparator(shouldSortInAscendingOrder()));
		} else if (shouldUseOrderedMap()) {
			innerMap = new LinkedHashMap(2);
		} else {
			innerMap = new HashMap(2);
		}
		return innerMap;
	}

	private boolean shouldPreserveExisting() {
		return (this.policy & POLICY_INNER_MAP_PRESERVE_EXISTING) == POLICY_INNER_MAP_PRESERVE_EXISTING;
	}

	private boolean shouldUseOrderedMap() {
		return (this.policy & POLICY_BOTH_MAPS_PRESERVE_ORDERING) == POLICY_BOTH_MAPS_PRESERVE_ORDERING;
	}

	private boolean shouldUseSortedInnerMap() {
		return (this.policy & POLICY_INNER_MAP_SORTED_MASK) != 0;
	}

	private boolean shouldSortInAscendingOrder() {
		return (this.policy & POLICY_INNER_MAP_SORTED_MASK) == POLICY_INNER_MAP_SORTED_ASCENDING;
	}

	private boolean shouldUseSingletonInnerMap() {
		return (this.policy & POLICY_INNER_MAP_ENSURE_SINGLETON) == POLICY_INNER_MAP_ENSURE_SINGLETON;
	}

	private static class ValueComparator implements Comparator, Serializable {
		private static final long serialVersionUID = 362497720873186266L;
		private boolean ascending;

		public ValueComparator(boolean ascending) {
			this.ascending = ascending;
		}

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object o1, Object o2) {
			try {
				if (o1 instanceof Comparable) {
					int cmp = ((Comparable) o1).compareTo(o2);
					return this.ascending ? cmp : (0 - cmp);
				}
			} catch (Exception e) {
				// Ignore
			}
			return 1;
		}
	}

}
