/*******************************************************************************
 * Copyright (c) 2010, 2017 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.query;

import java.lang.reflect.Array;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This class allows to adapt java collections to a p2 a query result  and as such something queryable  
 * @since 2.0
 */
public class CollectionResult<T> implements IQueryResult<T> {
	private final Collection<T> collection;

	public CollectionResult(Collection<T> collection) {
		this.collection = collection == null ? Collections.<T> emptySet() : collection;
	}

	@Override
	public IQueryResult<T> query(IQuery<T> query, IProgressMonitor monitor) {
		return query.perform(iterator());
	}

	@Override
	public boolean isEmpty() {
		return collection.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return collection.iterator();
	}

	@Override
	public T[] toArray(Class<T> clazz) {
		int size = collection.size();
		@SuppressWarnings("unchecked")
		T[] result = (T[]) Array.newInstance(clazz, size);
		if (size != 0)
			collection.toArray(result);
		return result;
	}

	@Override
	public Set<T> toSet() {
		return new HashSet<>(collection);
	}

	@Override
	public Set<T> toUnmodifiableSet() {
		return collection instanceof Set<?> ? Collections.<T> unmodifiableSet((Set<T>) collection) : toSet();
	}

	@Override
	public String toString() {
		return collection.toString();
	}
}
