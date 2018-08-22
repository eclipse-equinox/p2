/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core.helpers;

import java.util.*;

/**
 * A Properties collection that maintains the order of insertion.
 * <p>
 * This class is used to store properties similar to {@link java.util.Properties}.
 * In particular both keys and values are strings and must be not null.
 * However this class is somewhat simplified and does not implement Cloneable, 
 * Serializable and Hashtable.
 * <p>
 * In contrast to java.util.Properties this class maintains the order by which 
 * properties are added. This is implemented using a {@link LinkedHashMap}.
 * <p>
 * The class does not support default properties as they can be expressed by 
 * creating java.util.Properties hierarchies.
 */
public class OrderedProperties extends Dictionary<String, String> implements Map<String, String> {

	LinkedHashMap<String, String> propertyMap = null;

	public static OrderedProperties unmodifiableProperties(Map<String, String> properties) {
		return new UnmodifiableProperties(properties);
	}

	public OrderedProperties() {
		super();
	}

	public OrderedProperties(int size) {
		super();
		propertyMap = new LinkedHashMap<>(size);
	}

	public OrderedProperties(Map<String, String> properties) {
		super();
		propertyMap = new LinkedHashMap<>(properties.size());
		putAll(properties);
	}

	/**
	 * Set the property value.
	 * <p>
	 * If a property with the key already exists, the previous
	 * value is replaced. Otherwise a new property is added at
	 * the end collection.
	 * 
	 * @param key   must not be null
	 * @param value must not be null
	 * @return previous value associated with specified key, or <tt>null</tt>
	 *	       if there was no mapping for key.
	 */
	public Object setProperty(String key, String value) {
		init();
		return propertyMap.put(key.intern(), value);
	}

	public String getProperty(String key) {
		return (propertyMap == null ? null : propertyMap.get(key));
	}

	/**
	 *	Initialize the map.
	 */
	private void init() {
		if (propertyMap == null) {
			propertyMap = new LinkedHashMap<>();
		}
	}

	@Override
	public int size() {
		return propertyMap == null ? 0 : propertyMap.size();
	}

	@Override
	public boolean isEmpty() {
		return propertyMap == null ? true : propertyMap.isEmpty();
	}

	@Override
	public synchronized void clear() {
		propertyMap = null;
	}

	@Override
	public String put(String key, String value) {
		init();
		return propertyMap.put(key.intern(), value);
	}

	@Override
	public boolean containsKey(Object key) {
		return propertyMap != null ? propertyMap.containsKey(key) : false;
	}

	@Override
	public boolean containsValue(Object value) {
		return propertyMap != null ? propertyMap.containsValue(value) : false;
	}

	@Override
	public Set<Map.Entry<String, String>> entrySet() {
		return propertyMap != null ? propertyMap.entrySet() : Collections.emptySet();
	}

	@Override
	public String get(Object key) {
		return propertyMap != null ? propertyMap.get(key) : null;
	}

	@Override
	public Set<String> keySet() {
		return propertyMap != null ? propertyMap.keySet() : Collections.emptySet();
	}

	@Override
	public void putAll(Map<? extends String, ? extends String> arg0) {
		init();
		propertyMap.putAll(arg0);
	}

	@Override
	public String remove(Object key) {
		return propertyMap != null ? propertyMap.remove(key) : null;
	}

	@Override
	public Collection<String> values() {
		return propertyMap != null ? propertyMap.values() : Collections.emptyList();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o instanceof OrderedProperties) {
			OrderedProperties rhs = (OrderedProperties) o;
			if (rhs.propertyMap == this.propertyMap)
				return true;
			if (rhs.propertyMap == null)
				return this.propertyMap.isEmpty();
			else if (this.propertyMap == null)
				return rhs.isEmpty();
			return rhs.propertyMap.equals(this.propertyMap);
		}
		if (this.propertyMap == null) {
			if (o instanceof Map<?, ?>)
				return ((Map<?, ?>) o).isEmpty();
			return false;
		}
		return this.propertyMap.equals(o);
	}

	@Override
	public int hashCode() {
		return propertyMap == null || propertyMap.isEmpty() ? 0 : propertyMap.hashCode();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(propertyMap);
		return sb.toString();
	}

	private class StringsEnum implements Enumeration<String> {

		private final Iterator<String> iterator;

		public StringsEnum(Collection<String> elems) {
			this.iterator = elems.iterator();
		}

		@Override
		public boolean hasMoreElements() {
			return iterator.hasNext();
		}

		@Override
		public String nextElement() {
			return iterator.next();
		}
	}

	@Override
	public Enumeration<String> elements() {
		return new StringsEnum(propertyMap.values());
	}

	@Override
	public Enumeration<String> keys() {
		return new StringsEnum(propertyMap.keySet());
	}

	private static class UnmodifiableProperties extends OrderedProperties {

		UnmodifiableProperties(Map<String, String> properties) {
			super();
			for (Map.Entry<String, String> entry : properties.entrySet()) {
				super.put(entry.getKey(), entry.getValue());
			}
		}

		@Override
		public synchronized Object setProperty(String key, String value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public synchronized String put(String key, String value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public synchronized String remove(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public synchronized void putAll(Map<? extends String, ? extends String> t) {
			throw new UnsupportedOperationException();
		}

		@Override
		public synchronized void clear() {
			throw new UnsupportedOperationException();
		}

	}

}
