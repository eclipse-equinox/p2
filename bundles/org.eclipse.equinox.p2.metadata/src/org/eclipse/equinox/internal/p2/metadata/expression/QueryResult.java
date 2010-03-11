/*******************************************************************************
 * Copyright (c) 2009 - 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.lang.reflect.Array;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.query.*;

/**
 * A result optimized for dealing with iterators returned from
 * expression evaluation.
 */
public class QueryResult<T> implements IQueryResult<T> {

	private Iterator<T> iterator;
	private boolean firstUsed = false;

	public QueryResult(Iterator<T> iterator) {
		this.iterator = (iterator instanceof IRepeatableIterator<?>) ? iterator : RepeatableIterator.create(iterator);
	}

	/**
	 * Create an QueryResult based on the given iterator. The <code>oneShot</code> parameter
	 * can be set to <code>true</code> if the returned instance is expected to be perused
	 * only once. This will allow some optimizations since the result of the iteration doesn't
	 * need to be copied in preparation for a second iteration.
	 *
	 * @param iterator The iterator to use as the result iterator.
	 * @param oneShot True if the created instance is perused only once.
	 */
	public QueryResult(Iterator<T> iterator, boolean oneShot) {
		this.iterator = oneShot ? iterator : ((iterator instanceof IRepeatableIterator<?>) ? iterator : RepeatableIterator.create(iterator));
	}

	public QueryResult(Collection<T> collection) {
		this.iterator = RepeatableIterator.create(collection);
	}

	public boolean isEmpty() {
		return !iterator.hasNext();
	}

	public Iterator<T> iterator() {
		if (firstUsed) {
			if (iterator instanceof RepeatableIterator<?>)
				return ((RepeatableIterator<T>) iterator).getCopy();
			throw new IllegalStateException("The one shot iterator has been used"); //$NON-NLS-1$
		}
		firstUsed = true;
		return iterator;
	}

	@SuppressWarnings("unchecked")
	public T[] toArray(Class<T> clazz) {
		if (iterator instanceof IRepeatableIterator<?>) {
			Object provider = ((RepeatableIterator<T>) iterator).getIteratorProvider();
			if (provider.getClass().isArray())
				return (T[]) provider;

			if (provider instanceof Collector<?>)
				return ((Collector<T>) provider).toArray(clazz);

			Collection<T> c = (Collection<T>) provider;
			return c.toArray((T[]) Array.newInstance(clazz, c.size()));
		}

		// Build a collection from the current iterator and then use
		// that as the provider, should an iterator be queried after
		// this call.
		Iterator<T> iter = iterator();
		ArrayList<T> c = new ArrayList<T>();
		while (iter.hasNext())
			c.add(iter.next());
		iterator = RepeatableIterator.create(c);
		firstUsed = false;
		return c.toArray((T[]) Array.newInstance(clazz, c.size()));
	}

	@SuppressWarnings("unchecked")
	public Set<T> toSet() {
		if (iterator instanceof IRepeatableIterator<?>) {
			Object provider = ((RepeatableIterator<T>) iterator).getIteratorProvider();
			if (provider.getClass().isArray()) {
				T[] elems = (T[]) provider;
				int idx = elems.length;
				HashSet<T> copy = new HashSet<T>(idx);
				while (--idx >= 0)
					copy.add(elems[idx]);
				return copy;
			}
			if (provider instanceof Collector<?>)
				return ((Collector<T>) provider).toSet();
			if (provider instanceof Map<?, ?>)
				return new HashSet<T>((Set<T>) ((Map<?, ?>) provider).entrySet());
			return new HashSet<T>((Collection<T>) provider);
		}
		// Build a collection from the current iterator and then use
		// that as the provider, should an iterator be queried after
		// this call.
		Iterator<T> iter = iterator();
		HashSet<T> c = new HashSet<T>();
		while (iter.hasNext())
			c.add(iter.next());
		iterator = RepeatableIterator.create(c);
		firstUsed = false;
		return c;
	}

	public IQueryResult<T> query(IQuery<T> query, IProgressMonitor monitor) {
		return query.perform(iterator());
	}

	@SuppressWarnings("unchecked")
	public Set<T> toUnmodifiableSet() {
		if (iterator instanceof IRepeatableIterator<?>) {
			Object provider = ((RepeatableIterator<T>) iterator).getIteratorProvider();
			if (provider instanceof Collector<?>)
				return ((Collector<T>) provider).toUnmodifiableSet();

			if (provider instanceof Set<?>)
				return Collections.unmodifiableSet((Set<T>) provider);

			if (provider instanceof Map<?, ?>)
				return Collections.unmodifiableSet((Set<T>) ((Map<?, ?>) provider).entrySet());
		}
		return toSet();
	}
}
