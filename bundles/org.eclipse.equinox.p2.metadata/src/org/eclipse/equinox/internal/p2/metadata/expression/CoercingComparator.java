/*******************************************************************************
 * Copyright (c) 2010, 2023 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Comparator;

import org.eclipse.equinox.p2.metadata.Version;
import org.osgi.framework.FrameworkUtil;

/**
 * A comparator that performs coercion if needed before comparison.
 * 
 * @param <T> The type for the comparator.
 */
public abstract class CoercingComparator<T> {
	static class BooleanCoercer extends CoercingComparator<Boolean> {
		@Override
		public int compare(Boolean o1, Boolean o2) {
			return o1.booleanValue() == o2.booleanValue() ? 0 : (o1.booleanValue() ? 1 : -1);
		}

		@Override
		Boolean coerce(Object v) {
			if (v instanceof Boolean b)
				return b;
			if (v instanceof String s) {
				String sv = s.trim();
				if (sv.equalsIgnoreCase("true")) //$NON-NLS-1$
					return Boolean.TRUE;
				if (sv.equalsIgnoreCase("false")) //$NON-NLS-1$
					return Boolean.FALSE;
			}
			throw uncoercable(v);
		}

		@Override
		Class<Boolean> getCoerceClass() {
			return Boolean.class;
		}

		@Override
		int getCoercePrio() {
			return 7;
		}
	}

	static class ClassCoercer extends CoercingComparator<Class<?>> {
		@Override
		public int compare(Class<?> o1, Class<?> o2) {
			return o1.getName().compareTo(o2.getName());
		}

		@Override
		Class<?> coerce(Object v) {
			if (v instanceof Class<?>)
				return (Class<?>) v;
			if (v instanceof String s) {
				try {
					return FrameworkUtil.getBundle(CoercingComparator.class).loadClass(s.trim());
				} catch (Exception e) {
					//
				}
			}
			throw uncoercable(v);
		}

		@SuppressWarnings("unchecked")
		@Override
		Class<Class<?>> getCoerceClass() {
			Class<?> cls = Class.class;
			return (Class<Class<?>>) cls;
		}

		@Override
		int getCoercePrio() {
			return 11;
		}
	}

	static class FromStringCoercer<T extends Comparable<Object>> extends CoercingComparator<T> {
		private final Class<T> coerceClass;
		private final Constructor<T> constructor;

		public FromStringCoercer(Class<T> coerceClass, Constructor<T> constructor) {
			this.coerceClass = coerceClass;
			this.constructor = constructor;
		}

		@Override
		T coerce(Object v) {
			if (v instanceof String s) {
				try {
					return constructor.newInstance(s.trim());
				} catch (Exception e) {
					//
				}
			}
			throw uncoercable(v);
		}

		@Override
		int compare(T o1, T o2) {
			return o1.compareTo(o2);
		}

		@Override
		Class<T> getCoerceClass() {
			return coerceClass;
		}

		@Override
		int getCoercePrio() {
			return 0;
		}
	}

	static class IntegerCoercer extends CoercingComparator<Integer> {
		@Override
		public int compare(Integer o1, Integer o2) {
			return o1.compareTo(o2);
		}

		@Override
		Integer coerce(Object v) {
			if (v instanceof Integer i)
				return i;
			if (v instanceof Number n)
				return Integer.valueOf(n.intValue());
			if (v instanceof String s) {
				try {
					return Integer.valueOf(s.trim());
				} catch (NumberFormatException e) {
					//
				}
			}
			throw uncoercable(v);
		}

		@Override
		Class<Integer> getCoerceClass() {
			return Integer.class;
		}

		@Override
		int getCoercePrio() {
			return 6;
		}
	}

	static class LongCoercer extends CoercingComparator<Long> {
		@Override
		public int compare(Long o1, Long o2) {
			return o1.compareTo(o2);
		}

		@Override
		Long coerce(Object v) {
			if (v instanceof Long l)
				return l;
			if (v instanceof Number n)
				return Long.valueOf(n.longValue());
			if (v instanceof String s) {
				try {
					return Long.valueOf(s.trim());
				} catch (NumberFormatException e) {
					//
				}
			}
			throw uncoercable(v);
		}

		@Override
		Class<Long> getCoerceClass() {
			return Long.class;
		}

		@Override
		int getCoercePrio() {
			return 5;
		}
	}

	static class StringCoercer extends CoercingComparator<String> {
		@Override
		public int compare(String o1, String o2) {
			return o1.compareTo(o2);
		}

		@Override
		String coerce(Object v) {
			if (v instanceof Class<?>)
				return ((Class<?>) v).getName();
			return v.toString();
		}

		@Override
		Class<String> getCoerceClass() {
			return String.class;
		}

		@Override
		int getCoercePrio() {
			return 10;
		}
	}

	static class VersionCoercer extends CoercingComparator<Version> {
		@Override
		public int compare(Version o1, Version o2) {
			return o1.compareTo(o2);
		}

		@Override
		boolean canCoerceTo(Class<?> cls) {
			return Version.class.isAssignableFrom(cls);
		}

		@Override
		Version coerce(Object v) {
			if (v instanceof Version version)
				return version;
			if (v instanceof String s) {
				try {
					return Version.create(s);
				} catch (NumberFormatException e) {
					//
				}
			}
			throw uncoercable(v);
		}

		@Override
		Class<Version> getCoerceClass() {
			return Version.class;
		}

		@Override
		int getCoercePrio() {
			return 1;
		}
	}

	private static class SetAccessibleAction implements PrivilegedAction<Object> {
		private final AccessibleObject accessible;

		SetAccessibleAction(AccessibleObject accessible) {
			this.accessible = accessible;
		}

		@Override
		public Object run() {
			accessible.setAccessible(true);
			return null;
		}
	}

	private static CoercingComparator<?>[] coercers = { new ClassCoercer(), new BooleanCoercer(), new LongCoercer(),
			new IntegerCoercer(), new VersionCoercer(), new StringCoercer() };

	private static final Class<?>[] constructorType = new Class<?>[] { String.class };

	/**
	 * Finds the comparator for <code>a</code> and <code>b</code> and delegates the
	 * coercion/comparison to the comparator according to priority.
	 * 
	 * @param o1 the first object to be compared.
	 * @param o2 the second object to be compared.
	 * @return The result of the comparison
	 * @throws IllegalArgumentException if no comparator was found or if coercion
	 *                                  was impossible
	 * @see Comparator#compare(Object, Object)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <TA extends Object, TB extends Object> int coerceAndCompare(TA o1, TB o2)
			throws IllegalArgumentException {
		if (o1 == null || o2 == null)
			throw new IllegalArgumentException("Cannot compare null to anything"); //$NON-NLS-1$

		if (o1 instanceof Comparable c1 && o1.getClass().isAssignableFrom(o2.getClass()))
			return c1.compareTo(o2);

		if (o2 instanceof Comparable c2 && o2.getClass().isAssignableFrom(o1.getClass()))
			return -c2.compareTo(o1);

		CoercingComparator<TA> ca = getComparator(o1, o2);
		CoercingComparator<TB> cb = getComparator(o2, o1);
		return ca.getCoercePrio() <= cb.getCoercePrio() ? ca.compare(o1, ca.coerce(o2)) : cb.compare(cb.coerce(o1), o2);
	}

	/**
	 * Finds the comparator for <code>a</code> and <code>b</code> and delegates the
	 * coercion/equal to the comparator according to priority.
	 * 
	 * @param o1 the first object to be compared.
	 * @param o2 the second object to be compared.
	 * @return The result of the equality test
	 * @throws IllegalArgumentException if no comparator was found or if coercion
	 *                                  was impossible
	 * @see Object#equals(Object)
	 */
	public static <TA extends Object, TB extends Object> boolean coerceAndEquals(TA o1, TB o2)
			throws IllegalArgumentException {
		if (o1 == o2)
			return true;

		if (o1 == null || o2 == null)
			return false;

		if (o1.getClass() != o2.getClass()) {
			if (o1.getClass().isAssignableFrom(o2.getClass()))
				return o1.equals(o2);
			if (o2.getClass().isAssignableFrom(o1.getClass()))
				return o2.equals(o1);
			try {
				CoercingComparator<TA> ca = getComparator(o1, o2);
				CoercingComparator<TB> cb = getComparator(o2, o1);
				return ca.getCoercePrio() <= cb.getCoercePrio() ? o1.equals(ca.coerce(o2)) : o2.equals(cb.coerce(o1));
			} catch (IllegalArgumentException e) {
				//
			}
		}
		return o1.equals(o2);
	}

	/**
	 * Obtains the coercing comparator for the given <code>value</code>.
	 * 
	 * @param value The value
	 * @return The coercing comparator
	 */
	@SuppressWarnings({ "unchecked", "deprecation", "removal" })
	public static <V> CoercingComparator<V> getComparator(V value, Object v2) {
		Class<V> vClass = (Class<V>) value.getClass();
		CoercingComparator<?>[] carr = coercers;
		int idx = carr.length;
		while (--idx >= 0) {
			CoercingComparator<?> c = carr[idx];
			if (c.canCoerceTo(vClass)) {
				return (CoercingComparator<V>) c;
			}
		}

		if (value instanceof Comparable<?> && v2 instanceof String) {
			Class<Comparable<Object>> cClass = (Class<Comparable<Object>>) vClass;
			Constructor<Comparable<Object>> constructor;
			try {
				constructor = cClass.getConstructor(constructorType);
				if (!constructor.isAccessible())
					AccessController.doPrivileged(new SetAccessibleAction(constructor));
				synchronized (CoercingComparator.class) {
					int top = coercers.length;
					CoercingComparator<?>[] nc = new CoercingComparator<?>[top + 1];
					System.arraycopy(coercers, 0, nc, 1, top);
					CoercingComparator<V> cv = (CoercingComparator<V>) new FromStringCoercer<>(cClass, constructor);
					nc[0] = cv;
					coercers = nc;
					return cv;
				}
			} catch (Exception e) {
				//
			}
		}
		throw new IllegalArgumentException("No comparator for " + vClass.getName()); //$NON-NLS-1$
	}

	protected IllegalArgumentException uncoercable(Object v) {
		StringBuilder sb = new StringBuilder("Cannot coerce "); //$NON-NLS-1$
		if (v instanceof String) {
			sb.append('\'');
			sb.append(v);
			sb.append('\'');
		} else if (v instanceof Number) {
			sb.append("number "); //$NON-NLS-1$
			sb.append(v);
		} else {
			sb.append("an object of instance "); //$NON-NLS-1$
			sb.append(v.getClass().getName());
		}
		sb.append(" into a "); //$NON-NLS-1$
		sb.append(getCoerceClass().getName());
		return new IllegalArgumentException(sb.toString());
	}

	boolean canCoerceTo(Class<?> cls) {
		return cls == getCoerceClass();
	}

	abstract T coerce(Object v);

	abstract int compare(T o1, T o2);

	abstract Class<T> getCoerceClass();

	abstract int getCoercePrio();
}
