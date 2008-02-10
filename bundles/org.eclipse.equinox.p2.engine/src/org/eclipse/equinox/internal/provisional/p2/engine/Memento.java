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
package org.eclipse.equinox.internal.provisional.p2.engine;

import java.util.*;

public class Memento {
	private static final long serialVersionUID = 3257399736837461585L;
	private static final Collection simples = Arrays.asList(new Class[] {String.class, Integer.class, Long.class, Float.class, Double.class, Byte.class, Short.class, Character.class, Boolean.class});
	private static final Collection simpleArrays = Arrays.asList(new Class[] {String[].class, Integer[].class, Long[].class, Float[].class, Double[].class, Byte[].class, Short[].class, Character[].class, Boolean[].class});
	private static final Collection primitiveArrays = Arrays.asList(new Class[] {long[].class, int[].class, short[].class, char[].class, byte[].class, double[].class, float[].class, boolean[].class});

	Map mementoMap = new HashMap();

	public Object remove(String key) {
		if (key == null)
			throw new NullPointerException();

		// TODO: persist change
		return mementoMap.remove(key);
	}

	public Object put(String key, Object value) {
		if (key == null)
			throw new NullPointerException();

		validateValue(value);

		// TODO: persist change
		return mementoMap.put(key, value);
	}

	public Object get(String key) {
		if (key == null)
			throw new NullPointerException();

		return mementoMap.get(key);
	}

	public Enumeration getKeys() {
		return new Enumeration() {
			Iterator keysIterator = mementoMap.keySet().iterator();

			public boolean hasMoreElements() {
				return keysIterator.hasNext();
			}

			public Object nextElement() {
				return keysIterator.next();
			}
		};
	}

	private static void validateValue(Object value) {
		if (value == null)
			return;

		Class clazz = value.getClass();

		if (simples.contains(clazz))
			return;

		if (simpleArrays.contains(clazz) || primitiveArrays.contains(clazz))
			return;

		throw new IllegalArgumentException(clazz.getName());
	}
}
