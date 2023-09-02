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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;

/**
 * A result optimized for dealing with iterators returned from expression
 * evaluation.
 */
public class QueryResult<T> implements IQueryResult<T> {

	private final IRepeatableIterator<T> iterator;
	private boolean firstUse = true;

	/**
	 * Create an QueryResult based on the given iterator. The <code>oneShot</code>
	 * parameter can be set to <code>true</code> if the returned instance is
	 * expected to be perused only once. This will allow some optimizations since
	 * the result of the iteration doesn't need to be copied in preparation for a
	 * second iteration.
	 *
	 * @param iterator The iterator to use as the result iterator.
	 */
	public QueryResult(Iterator<T> iterator) {
		this.iterator = (iterator instanceof IRepeatableIterator<T> repeatable) //
				? repeatable
				: RepeatableIterator.create(iterator);
	}

	public QueryResult(Collection<T> collection) {
		this.iterator = RepeatableIterator.create(collection);
	}

	@Override
	public boolean isEmpty() {
		return !iterator.hasNext();
	}

	@Override
	public Iterator<T> iterator() {
		if (firstUse) {
			firstUse = false;
			return iterator;
		}
		return iterator.getCopy();
	}

	@Override
	@SuppressWarnings("unchecked")
	public T[] toArray(Class<T> clazz) {
		Object provider = iterator.getIteratorProvider();
		if (provider.getClass().isArray()) {
			return (T[]) provider;
		}
		return toArray(toUnmodifiableSet(), clazz);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<T> toSet() {
		Object provider = iterator.getIteratorProvider();
		if (provider instanceof Collection collection) {
			return new HashSet<>(collection);
		} else if (provider instanceof IIndexProvider indexProvider) {
			return iteratorToSet(indexProvider.everything());
		} else if (provider.getClass().isArray()) {
			T[] elems = (T[]) provider;
			return new HashSet<>(Arrays.asList(elems));
		} else if (provider instanceof Map<?, ?> map) {
			return new HashSet<>((Set<T>) map.entrySet());
		}
		return iteratorToSet(iterator());
	}

	@Override
	public IQueryResult<T> query(IQuery<T> query, IProgressMonitor monitor) {
		return query.perform(iterator());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<T> toUnmodifiableSet() {
		Object provider = iterator.getIteratorProvider();
		if (provider instanceof Set set) {
			return Collections.unmodifiableSet(set);
		} else if (provider instanceof Map map) {
			return Collections.unmodifiableSet((Set<T>) map.entrySet());
		}
		return toSet();
	}

	private Set<T> iteratorToSet(Iterator<T> iter) {
		Set<T> set = new HashSet<>();
		while (iter.hasNext()) {
			set.add(iter.next());
		}
		return set;
	}

	public static <T> T[] toArray(Collection<T> collection, Class<T> clazz) {
		@SuppressWarnings("unchecked")
		T[] arr = (T[]) Array.newInstance(clazz, collection == null ? 0 : collection.size());
		return collection != null ? collection.toArray(arr) : arr;
	}
}
