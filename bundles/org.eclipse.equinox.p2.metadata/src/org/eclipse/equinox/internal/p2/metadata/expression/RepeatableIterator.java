/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
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
		return iterator instanceof IRepeatableIterator<?> ? ((IRepeatableIterator<T>) iterator).getCopy() : new RepeatableIterator<>(iterator);
	}

	public static <T> IRepeatableIterator<T> create(Collection<T> values) {
		return new RepeatableIterator<>(values);
	}

	public static <T> IRepeatableIterator<T> create(IQueryResult<T> values) {
		return new QueryResultIterator<>(values);
	}

	public static <T> IRepeatableIterator<T> create(T[] values) {
		return new ArrayIterator<>(values);
	}

	public static <T> IRepeatableIterator<T> create(IIndexProvider<T> values) {
		return new IndexProviderIterator<>(values);
	}

	RepeatableIterator(Iterator<T> iterator) {
		HashSet<T> v = new HashSet<>();
		while (iterator.hasNext())
			v.add(iterator.next());
		values = v;
		this.iterator = v.iterator();
	}

	RepeatableIterator(Collection<T> values) {
		this.values = values;
		this.iterator = values.iterator();
	}

	@Override
	public IRepeatableIterator<T> getCopy() {
		return new RepeatableIterator<>(values);
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public T next() {
		return iterator.next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getIteratorProvider() {
		return values;
	}

	static class ArrayIterator<T> implements IRepeatableIterator<T> {
		private final T[] array;
		private int position = -1;

		public ArrayIterator(T[] array) {
			this.array = array;
		}

		@Override
		public Object getIteratorProvider() {
			return array;
		}

		@Override
		public boolean hasNext() {
			return position + 1 < array.length;
		}

		@Override
		public T next() {
			if (++position >= array.length)
				throw new NoSuchElementException();
			return array[position];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public IRepeatableIterator<T> getCopy() {
			return new ArrayIterator<>(array);
		}
	}

	static class IndexProviderIterator<T> implements IRepeatableIterator<T> {
		private final IIndexProvider<T> indexProvider;
		private final Iterator<T> iterator;

		IndexProviderIterator(IIndexProvider<T> indexProvider) {
			this.iterator = indexProvider.everything();
			this.indexProvider = indexProvider;
		}

		@Override
		public IRepeatableIterator<T> getCopy() {
			return new IndexProviderIterator<>(indexProvider);
		}

		@Override
		public Object getIteratorProvider() {
			return indexProvider;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public T next() {
			return iterator.next();
		}

		@Override
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

		@Override
		public IRepeatableIterator<T> getCopy() {
			return new QueryResultIterator<>(queryResult);
		}

		@Override
		public Object getIteratorProvider() {
			return queryResult;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public T next() {
			return iterator.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
