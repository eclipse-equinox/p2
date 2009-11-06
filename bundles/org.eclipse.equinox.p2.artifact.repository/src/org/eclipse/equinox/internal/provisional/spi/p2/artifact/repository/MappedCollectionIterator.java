/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository;

import java.util.*;

/**
 * An iterator over a Map, For each key we return the key, then the value.  If the value is a collection, then
 * instead of returning the value, we iterate over that collection before continuing to the 
 * next key.
 */
public class MappedCollectionIterator implements Iterator {
	private Iterator keys = null;
	private Iterator collection = null;
	private Object nextValue = null;
	private boolean returnKeys = false;
	private Map map = null;

	/**
	 * @param map - the map to iterate over
	 * @param returnKeys - whether or not to include the keys, if false return only values
	 */
	public MappedCollectionIterator(Map map, boolean returnKeys) {
		this.map = map;
		this.returnKeys = returnKeys;
		this.keys = map.keySet().iterator();
	}

	/**
	 * See {@link Iterator#hasNext()}
	 */
	public boolean hasNext() {
		if (returnKeys) //keys and values
			return (collection != null && collection.hasNext()) || nextValue != null || keys.hasNext();

		//values only
		if (collection != null && collection.hasNext())
			return true;
		while (keys.hasNext()) {
			Object key = keys.next();
			Object value = map.get(key);
			if (value instanceof Collection) {
				collection = ((Collection) value).iterator();
			} else if (value != null) {
				nextValue = value;
				collection = null;
				return true;
			}
			if (collection.hasNext())
				return true;
		}
		return false;
	}

	/**
	 * See {@link Iterator#next()}
	 */
	public Object next() {
		if (nextValue != null) {
			Object next = nextValue;
			nextValue = null;
			return next;
		}

		if (returnKeys) {
			if (collection != null && collection.hasNext())
				return collection.next();

			Object nextKey = keys.next();
			Object value = map.get(nextKey);
			if (value instanceof Collection)
				collection = ((Collection) value).iterator();
			else if (value != null) {
				nextValue = value;
				collection = null;
			}
			return nextKey;
		}

		//values only
		if (collection != null && collection.hasNext())
			return collection.next();
		while (keys.hasNext()) {
			Object key = keys.next();
			Object value = map.get(key);
			if (value instanceof Collection) {
				collection = ((Collection) value).iterator();
				if (!collection.hasNext())
					continue;
				return collection.next();
			} else if (value != null)
				return value;
		}
		throw new NoSuchElementException();
	}

	/**
	 * Returns true if the iteration contains more keys
	 * @return boolean
	 */
	public boolean hasNextKey() {
		if (!returnKeys)
			return false;
		return keys.hasNext();
	}

	/**
	 * Get the next key skipping over any values
	 * @return the next key
	 * @throws IllegalStateException if this iterator is not returning keys 
	 */
	public Object nextKey() {
		if (!returnKeys)
			throw new IllegalStateException();
		Object nextKey = keys.next();
		Object value = map.get(nextKey);
		if (value instanceof Collection) {
			collection = ((Collection) value).iterator();
			nextValue = null;
		} else if (value != null) {
			nextValue = value;
			collection = null;
		}
		return nextKey;
	}

	/**
	 * See {@link Iterator#remove()}
	 * This operation is not supported
	 * @throws UnsupportedOperationException
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}
}