/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.util.*;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.query.IQueryResult;

public class RepeatableIterator<T> implements IRepeatableIterator<T> {
	private final Collection<T> values;
	private final Iterator<T> iterator;

	@SuppressWarnings("unchecked")
	public static <T> IRepeatableIterator<T> create(Object unknown) {
		if (unknown.getClass().isArray())
			return create((T[]) unknown);
		if (unknown instanceof Iterator<?>)
			return create((Iterator<T>) unknown);
		if (unknown instanceof List<?>)
			return create((List<T>) unknown);
		if (unknown instanceof Collection<?>)
			return create((Collection<T>) unknown);
		if (unknown instanceof Map<?, ?>)
			return create((Set<T>) ((Map<?, ?>) unknown).entrySet());
		if (unknown instanceof IQueryResult<?>)
			return create((IQueryResult<T>) unknown);
		if (unknown instanceof IIndexProvider<?>)
			return create((IIndexProvider<T>) unknown);
		throw new IllegalArgumentException("Cannot convert a " + unknown.getClass().getName() + " into an iterator"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static <T> IRepeatableIterator<T> create(Iterator<T> iterator) {
		return iterator instanceof IRepeatableIterator<?> ? ((IRepeatableIterator<T>) iterator).getCopy() : new RepeatableIterator<T>(iterator);
	}

	public static <T> IRepeatableIterator<T> create(Collection<T> values) {
		return new RepeatableIterator<T>(values);
	}

	public static <T> IRepeatableIterator<T> create(IQueryResult<T> values) {
		return new QueryResultIterator<T>(values);
	}

	public static <T> IRepeatableIterator<T> create(T[] values) {
		return new ArrayIterator<T>(values);
	}

	public static <T> IRepeatableIterator<T> create(IIndexProvider<T> values) {
		return new IndexProviderIterator<T>(values);
	}

	RepeatableIterator(Iterator<T> iterator) {
		HashSet<T> v = new HashSet<T>();
		while (iterator.hasNext())
			v.add(iterator.next());
		values = v;
		this.iterator = v.iterator();
	}

	RepeatableIterator(Collection<T> values) {
		this.values = values;
		this.iterator = values.iterator();
	}

	public IRepeatableIterator<T> getCopy() {
		return new RepeatableIterator<T>(values);
	}

	public boolean hasNext() {
		return iterator.hasNext();
	}

	public T next() {
		return iterator.next();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public Object getIteratorProvider() {
		return values;
	}

	static class ArrayIterator<T> implements IRepeatableIterator<T> {
		private final T[] array;
		private int position = -1;

		public ArrayIterator(T[] array) {
			this.array = array;
		}

		public Object getIteratorProvider() {
			return array;
		}

		public boolean hasNext() {
			return position + 1 < array.length;
		}

		public T next() {
			if (++position >= array.length)
				throw new NoSuchElementException();
			return array[position];
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public IRepeatableIterator<T> getCopy() {
			return new ArrayIterator<T>(array);
		}
	}

	static class IndexProviderIterator<T> implements IRepeatableIterator<T> {
		private final IIndexProvider<T> indexProvider;
		private final Iterator<T> iterator;

		IndexProviderIterator(IIndexProvider<T> indexProvider) {
			this.iterator = indexProvider.everything();
			this.indexProvider = indexProvider;
		}

		public IRepeatableIterator<T> getCopy() {
			return new IndexProviderIterator<T>(indexProvider);
		}

		public Object getIteratorProvider() {
			return indexProvider;
		}

		public boolean hasNext() {
			return iterator.hasNext();
		}

		public T next() {
			return iterator.next();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	static class QueryResultIterator<T> implements IRepeatableIterator<T> {
		private final IQueryResult<T> queryResult;

		private final Iterator<T> iterator;

		QueryResultIterator(IQueryResult<T> queryResult) {
			this.queryResult = queryResult;
			this.iterator = queryResult.iterator();
		}

		public IRepeatableIterator<T> getCopy() {
			return new QueryResultIterator<T>(queryResult);
		}

		public Object getIteratorProvider() {
			return queryResult;
		}

		public boolean hasNext() {
			return iterator.hasNext();
		}

		public T next() {
			return iterator.next();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
