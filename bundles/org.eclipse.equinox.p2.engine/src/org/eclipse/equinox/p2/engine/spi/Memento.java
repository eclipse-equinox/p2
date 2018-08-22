/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine.spi;

import java.util.*;

/**
 * @since 2.0
 */
public final class Memento {
	private static final Collection<Class<?>> simples = Arrays.asList(String.class, Integer.class, Long.class, Float.class, Double.class, Byte.class, Short.class, Character.class, Boolean.class);
	private static final Collection<Class<?>> simpleArrays = Arrays.asList(String[].class, Integer[].class, Long[].class, Float[].class, Double[].class, Byte[].class, Short[].class, Character[].class, Boolean[].class);
	private static final Collection<Class<?>> primitiveArrays = Arrays.asList(long[].class, int[].class, short[].class, char[].class, byte[].class, double[].class, float[].class, boolean[].class);

	Map<String, Object> mementoMap = new HashMap<>();

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

	public Enumeration<String> getKeys() {
		return new Enumeration<String>() {
			Iterator<String> keysIterator = mementoMap.keySet().iterator();

			@Override
			public boolean hasMoreElements() {
				return keysIterator.hasNext();
			}

			@Override
			public String nextElement() {
				return keysIterator.next();
			}
		};
	}

	private static void validateValue(Object value) {
		if (value == null)
			return;

		Class<? extends Object> clazz = value.getClass();

		if (simples.contains(clazz))
			return;

		if (simpleArrays.contains(clazz) || primitiveArrays.contains(clazz))
			return;

		throw new IllegalArgumentException(clazz.getName());
	}
}
