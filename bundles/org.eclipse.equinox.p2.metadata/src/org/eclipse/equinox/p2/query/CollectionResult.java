/*******************************************************************************
 * Copyright (c) 2010, 2018 Cloudsmith Inc. and others.
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
package org.eclipse.equinox.p2.query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.expression.QueryResult;

/**
 * This class allows to adapt java collections to a p2 a query result and as
 * such something queryable
 * 
 * @since 2.0
 */
public class CollectionResult<T> implements IQueryResult<T> {
	private final Collection<T> collection;

	public CollectionResult(Collection<T> collection) {
		this.collection = collection == null ? Collections.emptySet() : collection;
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
		return QueryResult.toArray(collection, clazz);
	}

	@Override
	public Set<T> toSet() {
		return new HashSet<>(collection);
	}

	@Override
	public Set<T> toUnmodifiableSet() {
		return collection instanceof Set<?> ? Collections.unmodifiableSet((Set<T>) collection) : toSet();
	}

	@Override
	public String toString() {
		return collection.toString();
	}

	@Override
	public Stream<T> stream() {
		return collection.stream();
	}
}
