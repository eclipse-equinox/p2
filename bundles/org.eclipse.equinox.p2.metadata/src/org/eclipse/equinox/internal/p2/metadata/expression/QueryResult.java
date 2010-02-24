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

	private final IRepeatableIterator<T> iterator;

	public QueryResult(Iterator<T> iterator) {
		this.iterator = RepeatableIterator.create(iterator);
	}

	public QueryResult(Collection<T> collection) {
		this.iterator = RepeatableIterator.create(collection);
	}

	public boolean isEmpty() {
		return !iterator.hasNext();
	}

	public Iterator<T> iterator() {
		return iterator.getCopy();
	}

	@SuppressWarnings("unchecked")
	public T[] toArray(Class<T> clazz) {
		Object provider = iterator.getIteratorProvider();
		if (provider.getClass().isArray())
			return (T[]) provider;

		if (provider instanceof Collector<?>)
			return ((Collector<T>) provider).toArray(clazz);

		Collection<T> c = (Collection<T>) provider;
		return c.toArray((T[]) Array.newInstance(clazz, c.size()));
	}

	@SuppressWarnings("unchecked")
	public Set<T> toSet() {
		Object provider = iterator.getIteratorProvider();
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

	public IQueryResult<T> query(IQuery<T> query, IProgressMonitor monitor) {
		return query.perform(iterator());
	}

	@SuppressWarnings("unchecked")
	public Set<T> unmodifiableSet() {
		Object provider = iterator.getIteratorProvider();
		if (provider instanceof Collector<?>)
			return ((Collector<T>) provider).unmodifiableSet();

		if (provider instanceof Set<?>)
			return Collections.unmodifiableSet((Set<T>) provider);

		if (provider instanceof Map<?, ?>)
			return Collections.unmodifiableSet((Set<T>) ((Map<?, ?>) provider).entrySet());

		return toSet();
	}
}
