/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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
		propertyMap = new LinkedHashMap<String, String>(size);
	}

	public OrderedProperties(OrderedProperties properties) {
		super();
		propertyMap = new LinkedHashMap<String, String>(properties.size());
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
		return propertyMap.put(key, value);
	}

	public String getProperty(String key) {
		return (propertyMap == null ? null : propertyMap.get(key));
	}

	public void putAll(OrderedProperties properties) {
		putAll(properties);
	}

	/**
	 *	Initialize the map.
	 */
	private void init() {
		if (propertyMap == null) {
			propertyMap = new LinkedHashMap<String, String>();
		}
	}

	public int size() {
		return propertyMap == null ? 0 : propertyMap.size();
	}

	public boolean isEmpty() {
		return propertyMap == null ? true : propertyMap.isEmpty();
	}

	public synchronized void clear() {
		propertyMap = null;
	}

	public String put(String key, String value) {
		init();
		return propertyMap.put(key, value);
	}

	public boolean containsKey(Object key) {
		return propertyMap != null ? propertyMap.containsKey(key) : false;
	}

	public boolean containsValue(Object value) {
		return propertyMap != null ? propertyMap.containsValue(value) : false;
	}

	public Set<Map.Entry<String, String>> entrySet() {
		return propertyMap != null ? propertyMap.entrySet() : Collections.<Map.Entry<String, String>> emptySet();
	}

	public String get(Object key) {
		return propertyMap != null ? propertyMap.get(key) : null;
	}

	public Set<String> keySet() {
		return propertyMap != null ? propertyMap.keySet() : Collections.<String> emptySet();
	}

	public void putAll(Map<? extends String, ? extends String> arg0) {
		init();
		propertyMap.putAll(arg0);
	}

	public String remove(Object key) {
		return propertyMap != null ? propertyMap.remove(key) : null;
	}

	public Collection<String> values() {
		return propertyMap != null ? propertyMap.values() : Collections.<String> emptyList();
	}

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

	public int hashCode() {
		return propertyMap == null || propertyMap.isEmpty() ? 0 : propertyMap.hashCode();
	}

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

		public boolean hasMoreElements() {
			return iterator.hasNext();
		}

		public String nextElement() {
			return iterator.next();
		}
	}

	public Enumeration<String> elements() {
		return new StringsEnum(propertyMap.values());
	}

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

		public synchronized Object setProperty(String key, String value) {
			throw new UnsupportedOperationException();
		}

		public synchronized String put(String key, String value) {
			throw new UnsupportedOperationException();
		}

		public synchronized String remove(Object key) {
			throw new UnsupportedOperationException();
		}

		public synchronized void putAll(Map<? extends String, ? extends String> t) {
			throw new UnsupportedOperationException();
		}

		public synchronized void clear() {
			throw new UnsupportedOperationException();
		}

	}

}
