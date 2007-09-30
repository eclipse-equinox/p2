/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.core.helpers;

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
public class OrderedProperties implements Map {

	private LinkedHashMap propertyMap = null;

	private static final String[] NO_KEYS = new String[0];

	public OrderedProperties() {
		super();
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
		return (String) (propertyMap == null ? null : propertyMap.get(key));
	}

	public String getProperty(String key, String defaultValue) {
		String value = getProperty(key);
		return (value == null) ? defaultValue : value;
	}

	public void putAll(OrderedProperties properties) {
		putAll((Map) properties);
	}

	public Collection getPropertyKeysCollection() {
		if (propertyMap == null)
			return Collections.EMPTY_LIST;
		return Collections.unmodifiableCollection(propertyMap.keySet());
	}

	public String[] getPropertyKeys() {
		if (propertyMap == null)
			return NO_KEYS;
		Collection keySet = propertyMap.keySet();
		return (String[]) keySet.toArray(new String[keySet.size()]);
	}

	/**
	 *	Initialize the map.
	 */
	private void init() {
		if (propertyMap == null) {
			propertyMap = new LinkedHashMap();
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

	public Object put(Object arg0, Object arg1) {
		init();
		return propertyMap.put(arg0, arg1);
	}

	public boolean containsKey(Object key) {
		return propertyMap != null ? propertyMap.containsKey(key) : false;
	}

	public boolean containsValue(Object value) {
		return propertyMap != null ? propertyMap.containsValue(value) : false;
	}

	public Set entrySet() {
		return propertyMap != null ? propertyMap.entrySet() : Collections.EMPTY_SET;
	}

	public Object get(Object key) {
		return propertyMap != null ? propertyMap.get(key) : null;
	}

	public Set keySet() {
		return propertyMap != null ? propertyMap.keySet() : Collections.EMPTY_SET;
	}

	public void putAll(Map arg0) {
		init();
		propertyMap.putAll(arg0);
	}

	public Object remove(Object key) {
		return propertyMap != null ? propertyMap.remove(key) : null;
	}

	public Collection values() {
		return propertyMap != null ? propertyMap.values() : Collections.EMPTY_LIST;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o instanceof OrderedProperties) {
			OrderedProperties rhs = (OrderedProperties) o;
			if (rhs.propertyMap == this.propertyMap)
				return true;
			if (rhs.propertyMap == null || this.propertyMap == null)
				return false;
			return rhs.propertyMap.equals(this.propertyMap);
		}
		return propertyMap.equals(o);
	}

	public int hashCode() {
		return propertyMap != null ? propertyMap.hashCode() : 0;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(propertyMap);
		return sb.toString();
	}

}
