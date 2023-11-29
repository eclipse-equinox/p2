/*******************************************************************************
 * Copyright (c) 2009, 2018 Cloudsmith Inc. and others.
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
package org.eclipse.equinox.internal.p2.core.helpers;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Helper class that provides some useful support when dealing with collections.
 */
public class CollectionUtils {

	/**
	 * A unmodifiable {@link List} implementation on top of an object array.
	 * @param <E> The element type.
	 */
	private static class UnmodifiableArrayList<E> extends AbstractList<E> implements RandomAccess, java.io.Serializable {
		private static final long serialVersionUID = 7435304230643855579L;
		final E[] array;

		UnmodifiableArrayList(E[] array) {
			this.array = array;
		}

		@Override
		public boolean contains(Object o) {
			return indexOf(o) != -1;
		}

		@Override
		public E get(int index) {
			return array[index];
		}

		@Override
		public int indexOf(Object o) {
			int size = array.length;
			if (o == null) {
				for (int i = 0; i < size; i++)
					if (array[i] == null)
						return i;
			} else {
				for (int i = 0; i < size; i++)
					if (o.equals(array[i]))
						return i;
			}
			return -1;
		}

		@Override
		public Iterator<E> iterator() {
			return listIterator();
		}

		/**
		 * Rapid iterator, motivated by the fact that we don't need to check
		 * for concurrent modifications.
		 */
		@Override
		public ListIterator<E> listIterator() {
			return new ListIterator<>() {
				private int index = 0;

				@Override
				public boolean hasNext() {
					return index < array.length;
				}

				@Override
				public E next() {
					if (index >= array.length)
						throw new NoSuchElementException();
					return array[index++];
				}

				@Override
				public boolean hasPrevious() {
					return index > 0;
				}

				@Override
				public E previous() {
					if (--index < 0) {
						++index;
						throw new NoSuchElementException();
					}
					return array[index];
				}

				@Override
				public int nextIndex() {
					return index;
				}

				@Override
				public int previousIndex() {
					return index - 1;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public void set(E e) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void add(E e) {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public int lastIndexOf(Object o) {
			int idx = array.length;
			if (o == null) {
				while (--idx >= 0)
					if (array[idx] == null)
						return idx;
			} else {
				while (--idx >= 0)
					if (o.equals(array[idx]))
						return idx;
			}
			return -1;
		}

		@Override
		public E set(int index, E element) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int size() {
			return array.length;
		}

		@Override
		public Object[] toArray() {
			return array.clone();
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T[] toArray(T[] a) {
			int size = array.length;
			if (a.length < size)
				a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
			System.arraycopy(this.array, 0, a, 0, size);
			while (size < a.length)
				a[size++] = null;
			return a;
		}
	}

	/**
	 * Creates a combined hash for an array of objects.
	 * @param objects The objects to hash
	 * @return The combined hashCode of the objects.
	 */
	public static int hashCode(Object objects[]) {
		if (objects == null)
			return 0;

		int result = 1;
		int idx = objects.length;
		while (--idx >= 0) {
			Object object = objects[idx];
			result = 17 * result + (object == null ? 0 : object.hashCode());
		}
		return result;
	}

	/**
	 * Returns an unmodifiable list that is backed by the <code>array</code>.
	 * @param <T> The list element type
	 * @param array The array of elements
	 * @return The unmodifiable list
	 */
	public static <T> List<T> unmodifiableList(T[] array) {
		return array == null || array.length == 0 ? Collections.emptyList() : new UnmodifiableArrayList<>(array);
	}

	/**
	 * Reads a property list using the {@link Properties#load(InputStream)} method. The
	 * properties are stored in a map.
	 * @param stream The stream to read from
	 * @return The resulting map
	 * @throws IOException propagated from the load method.
	 */
	public static Map<String, String> loadProperties(InputStream stream) throws IOException {
		Properties properties = new Properties();
		properties.load(stream);
		return toMap(properties);
	}

	/**
	 * Copies all elements from <code>properties</code> into a Map. The returned map might be unmodifiable
	 * @return The map containing all elements
	 */
	public static Map<String, String> toMap(Properties properties) {
		if (properties == null || properties.isEmpty())
			return Collections.emptyMap();

		Map<String, String> props = new HashMap<>(properties.size());
		putAll(properties, props);
		return props;
	}

	/**
	 * Copies all elements from <code>properties</code> into the given <code>result</code>.
	 */
	public static void putAll(Properties properties, Map<String, String> result) {
		for (Enumeration<Object> keys = properties.keys(); keys.hasMoreElements();) {
			String key = (String) keys.nextElement();
			result.put(key, properties.getProperty(key));
		}
	}

	/**
	 * Stores the properties using {@link Properties#store(OutputStream, String)} on the given <code>stream</code>.
	 * @param properties The properties to store
	 * @param stream The stream to store to
	 * @param comment The comment to use
	 */
	public static void storeProperties(Map<String, String> properties, OutputStream stream, String comment) throws IOException {
		Properties props = new Properties();
		props.putAll(properties);
		props.store(stream, comment);
	}
}
