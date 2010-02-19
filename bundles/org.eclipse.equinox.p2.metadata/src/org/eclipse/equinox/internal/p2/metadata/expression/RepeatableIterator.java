/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
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
	private final List<T> values;
	private int position = -1;

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
		throw new IllegalArgumentException("Cannot convert a " + unknown.getClass().getName() + " into an iterator"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static <T> IRepeatableIterator<T> create(Iterator<T> iterator) {
		return iterator instanceof IRepeatableIterator<?> ? ((IRepeatableIterator<T>) iterator).getCopy() : new ElementRetainingIterator<T>(iterator);
	}

	public static <T> IRepeatableIterator<T> create(List<T> values) {
		return new RepeatableIterator<T>(values);
	}

	public static <T> IRepeatableIterator<T> create(Collection<T> values) {
		return new CollectionIterator<T>(values);
	}

	public static <T> IRepeatableIterator<T> create(IQueryResult<T> values) {
		return new QueryResultIterator<T>(values);
	}

	public static <T> IRepeatableIterator<T> create(T[] values) {
		return new ArrayIterator<T>(values);
	}

	RepeatableIterator(List<T> values) {
		this.values = values;
	}

	public IRepeatableIterator<T> getCopy() {
		return new RepeatableIterator<T>(values);
	}

	public boolean hasNext() {
		return position + 1 < values.size();
	}

	public T next() {
		if (++position == values.size()) {
			--position;
			throw new NoSuchElementException();
		}
		return values.get(position);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public Object getIteratorProvider() {
		return values;
	}

	void setPosition(int position) {
		this.position = position;
	}

	List<T> getValues() {
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

	static abstract class DeferredIterator<T> implements IRepeatableIterator<T> {
		private Iterator<T> iterator;

		public boolean hasNext() {
			if (iterator == null)
				iterator = getIterator();
			return iterator.hasNext();
		}

		public T next() {
			if (iterator == null)
				iterator = getIterator();
			return iterator.next();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		abstract Iterator<T> getIterator();
	}

	static class CollectionIterator<T> extends DeferredIterator<T> {
		private final Collection<T> collection;

		CollectionIterator(Collection<T> collection) {
			this.collection = collection;
		}

		public IRepeatableIterator<T> getCopy() {
			return new CollectionIterator<T>(collection);
		}

		public Object getIteratorProvider() {
			return collection;
		}

		Iterator<T> getIterator() {
			return collection.iterator();
		}
	}

	static class IndexProviderIterator<T> extends DeferredIterator<T> {
		private final IIndexProvider<T> indexProvider;

		IndexProviderIterator(IIndexProvider<T> indexProvider) {
			this.indexProvider = indexProvider;
		}

		public IRepeatableIterator<T> getCopy() {
			return new IndexProviderIterator<T>(indexProvider);
		}

		public Object getIteratorProvider() {
			return indexProvider;
		}

		Iterator<T> getIterator() {
			return indexProvider.everything();
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

	static class ElementRetainingIterator<T> extends RepeatableIterator<T> {

		private Iterator<T> innerIterator;

		ElementRetainingIterator(Iterator<T> iterator) {
			super(new ArrayList<T>());
			innerIterator = iterator;
		}

		public synchronized boolean hasNext() {
			if (innerIterator != null) {
				if (innerIterator.hasNext())
					return true;
				innerIterator = null;
				setPosition(getValues().size());
			}
			return super.hasNext();
		}

		public synchronized T next() {
			if (innerIterator != null) {
				T val = innerIterator.next();
				getValues().add(val);
				return val;
			}
			return super.next();
		}

		public synchronized IRepeatableIterator<T> getCopy() {
			// If the current iterator still exists, we must exhaust it first
			//
			exhaustInnerIterator();
			return super.getCopy();
		}

		public synchronized Object getIteratorProvider() {
			exhaustInnerIterator();
			return super.getIteratorProvider();
		}

		private void exhaustInnerIterator() {
			if (innerIterator != null) {
				List<T> values = getValues();
				int savePos = values.size() - 1;
				while (innerIterator.hasNext())
					values.add(innerIterator.next());
				innerIterator = null;
				setPosition(savePos);
			}
		}
	}
}
