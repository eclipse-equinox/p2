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
package org.eclipse.equinox.p2.engine;

import java.util.*;

public class Memento extends HashMap {
	private static final long serialVersionUID = 3257399736837461585L;
	private static final Collection simples = Arrays.asList(new Class[] {String.class, Integer.class, Long.class, Float.class, Double.class, Byte.class, Short.class, Character.class, Boolean.class});
	private static final Collection simpleArrays = Arrays.asList(new Class[] {String[].class, Integer[].class, Long[].class, Float[].class, Double[].class, Byte[].class, Short[].class, Character[].class, Boolean[].class});
	private static final Collection primitiveArrays = Arrays.asList(new Class[] {long[].class, int[].class, short[].class, char[].class, byte[].class, double[].class, float[].class, boolean[].class});

	public Object remove(Object key) {
		// TODO: persist change
		return super.remove(key);
	}

	public Object put(Object key, Object value) {
		validateKey(key);
		validateValue(value);

		// TODO: persist change
		return super.put(key, value);
	}

	private static void validateKey(Object key) {
		if (key == null)
			throw new NullPointerException();

		Class clazz = key.getClass();
		if (clazz == String.class)
			return;

		throw new IllegalArgumentException(clazz.getName());
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
