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
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;

/**
 * A result optimized for dealing with iterators returned from
 * expression evaluation.
 */
public class QueryResult<T> implements IQueryResult<T> {

	private final IRepeatableIterator<T> iterator;
	private boolean firstUse = true;

	/**
	 * Create an QueryResult based on the given iterator. The <code>oneShot</code> parameter
	 * can be set to <code>true</code> if the returned instance is expected to be perused
	 * only once. This will allow some optimizations since the result of the iteration doesn't
	 * need to be copied in preparation for a second iteration.
	 *
	 * @param iterator The iterator to use as the result iterator.
	 */
	public QueryResult(Iterator<T> iterator) {
		this.iterator = (iterator instanceof IRepeatableIterator<?>) ? (IRepeatableIterator<T>) iterator : RepeatableIterator.create(iterator);
	}

	public QueryResult(Collection<T> collection) {
		this.iterator = RepeatableIterator.create(collection);
	}

	public boolean isEmpty() {
		return !iterator.hasNext();
	}

	public Iterator<T> iterator() {
		if (firstUse) {
			firstUse = false;
			return iterator;
		}
		return iterator.getCopy();
	}

	@SuppressWarnings("unchecked")
	public T[] toArray(Class<T> clazz) {
		Object provider = iterator.getIteratorProvider();
		if (provider.getClass().isArray())
			return (T[]) provider;

		Collection<T> c = toUnmodifiableSet();
		return c.toArray((T[]) Array.newInstance(clazz, c.size()));
	}

	@SuppressWarnings("unchecked")
	public Set<T> toSet() {
		Object provider = iterator.getIteratorProvider();
		if (provider instanceof Collection<?>)
			return new HashSet<T>((Collection<T>) provider);
		if (provider instanceof IIndexProvider<?>)
			return iteratorToSet(((IIndexProvider<T>) provider).everything());
		if (provider.getClass().isArray()) {
			T[] elems = (T[]) provider;
			int idx = elems.length;
			HashSet<T> copy = new HashSet<T>(idx);
			while (--idx >= 0)
				copy.add(elems[idx]);
			return copy;
		}
		if (provider instanceof Map<?, ?>)
			return new HashSet<T>((Set<T>) ((Map<?, ?>) provider).entrySet());
		return iteratorToSet(iterator());
	}

	public IQueryResult<T> query(IQuery<T> query, IProgressMonitor monitor) {
		return query.perform(iterator());
	}

	@SuppressWarnings("unchecked")
	public Set<T> toUnmodifiableSet() {
		Object provider = iterator.getIteratorProvider();
		if (provider instanceof Set<?>)
			return Collections.unmodifiableSet((Set<T>) provider);
		if (provider instanceof Map<?, ?>)
			return Collections.unmodifiableSet((Set<T>) ((Map<?, ?>) provider).entrySet());
		return toSet();
	}

	private Set<T> iteratorToSet(Iterator<T> iter) {
		HashSet<T> set = new HashSet<T>();
		while (iter.hasNext())
			set.add(iter.next());
		return set;
	}
}
