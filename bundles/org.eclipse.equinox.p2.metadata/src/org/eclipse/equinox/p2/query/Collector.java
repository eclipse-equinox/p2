/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.Messages;
import org.eclipse.equinox.internal.p2.metadata.expression.QueryResult;

/**
 * A collector is a generic visitor that collects objects passed to it, and can
 * then express the result of the visit in various forms. The collector can also
 * short-circuit a traversal by returning <code>false</code> from its
 * {@link #accept(Object)} method.
 * <p>
 * This default collector just accepts all objects passed to it. Clients may
 * subclass to perform different processing on the objects passed to it.
 * 
 * @param <T> The type of object accepted by this collector
 * @since 2.0
 */
public class Collector<T> implements IQueryResult<T> {
	private Set<T> collected = null;

	public static final Collector<?> EMPTY_COLLECTOR = new Collector<>() {
		@Override
		public boolean accept(Object val) {
			return false;
		}
	};

	@SuppressWarnings("unchecked")
	public static final <T> Collector<T> emptyCollector() {
		return (Collector<T>) EMPTY_COLLECTOR;
	}

	/**
	 * Accepts an object.
	 * <p>
	 * This default implementation adds the objects to a list. Clients may override
	 * this method to perform additional filtering, add different objects to the
	 * list, short-circuit the traversal, or process the objects directly without
	 * collecting them.
	 * 
	 * @param object the object to collect or visit
	 * @return <code>true</code> if the traversal should continue, or
	 *         <code>false</code> to indicate the traversal should stop.
	 */
	public boolean accept(T object) {
		getCollection().add(object);
		return true;
	}

	/**
	 * Adds the elements from one collector to this collector
	 * 
	 * @param queryResult The collector from which the elements should be retrieved
	 */
	public void addAll(IQueryResult<T> queryResult) {
		boolean keepGoing = true;
		for (Iterator<T> iter = queryResult.iterator(); iter.hasNext() && keepGoing;) {
			keepGoing = accept(iter.next());
		}
	}

	/**
	 * Returns the collection that is being used to collect results. Unlike
	 * {@linkplain #toSet()}, this returns the actual modifiable collection that is
	 * being used to store results. The return value is only intended to be used
	 * within subclasses and should not be exposed outside of a collection class.
	 * 
	 * @return the collection being used to collect results.
	 */
	protected Collection<T> getCollection() {
		if (collected == null) {
			collected = new HashSet<>();
		}
		return collected;
	}

	/**
	 * Returns whether this collector is empty.
	 * 
	 * @return <code>true</code> if this collector has accepted any results, and
	 *         <code>false</code> otherwise.
	 */
	@Override
	public boolean isEmpty() {
		return collected == null || collected.isEmpty();
	}

	/**
	 * Returns an iterator on the collected objects.
	 * 
	 * @return an iterator of the collected objects.
	 */
	@Override
	public Iterator<T> iterator() {
		return collected == null ? Collections.emptyIterator() : collected.iterator();
	}

	/**
	 * Returns the number of collected objects.
	 */
	public int size() {
		return collected == null ? 0 : collected.size();
	}

	/**
	 * Returns the collected objects as an array
	 * 
	 * @param clazz The type of array to return
	 * @return The array of results
	 * @throws ArrayStoreException the runtime type of the specified array is not a
	 *                             super-type of the runtime type of every collected
	 *                             object
	 */
	@Override
	public T[] toArray(Class<T> clazz) {
		return QueryResult.toArray(collected, clazz);
	}

	/**
	 * Returns a copy of the collected objects.
	 * 
	 * @return An modifiable collection of the collected objects
	 */
	@Override
	public Set<T> toSet() {
		return collected == null ? new HashSet<>() : new HashSet<>(collected);
	}

	/**
	 * Performs a query on this results of this collector.
	 */
	@Override
	public IQueryResult<T> query(IQuery<T> query, IProgressMonitor monitor) {
		IQueryResult<T> result;
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		try {
			monitor.beginTask(Messages.performing_subquery, 1);
			result = query.perform(iterator());
			monitor.worked(1);
		} finally {
			monitor.done();
		}
		return result;
	}

	/**
	 * Returns the collected objects as an immutable collection.
	 * 
	 * @return An unmodifiable collection of the collected objects
	 */
	@Override
	public Set<T> toUnmodifiableSet() {
		if (collected == null) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(collected);
	}

	@Override
	public Stream<T> stream() {
		return collected == null ? Stream.empty() : collected.stream();
	}
}
