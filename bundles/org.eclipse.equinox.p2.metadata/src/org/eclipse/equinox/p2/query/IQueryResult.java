/******************************************************************************* 
* Copyright (c) 2009, 2017 EclipseSource and others.
 *
 * This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
* available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.query;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An IQueryResult represents the results of a query.
 * 
 * @since 2.0
 */
public interface IQueryResult<T> extends IQueryable<T>, Iterable<T> {
	/**
	 * Returns whether this QueryResult is empty.
	 * 
	 * @return <code>true</code> if this QueryResult has accepted any results, and
	 *         <code>false</code> otherwise.
	 */
	boolean isEmpty();

	/**
	 * Returns an iterator on the collected objects.
	 * 
	 * @return an iterator of the collected objects.
	 */
	@Override
	Iterator<T> iterator();

	/**
	 * Returns the collected objects as an array
	 * 
	 * @param clazz The type of array to return
	 * @return The array of results
	 * @throws ArrayStoreException the runtime type of the specified array is
	 *         not a super-type of the runtime type of every collected object
	 */
	T[] toArray(Class<T> clazz);

	/**
	 * Creates a new Set copy with the contents of this query result. The copy can
	 * be altered without any side effects on its origin.
	 * 
	 * @return A detached copy of the result.
	 */
	Set<T> toSet();

	/**
	 * Returns a Set backed by this query result. The set is immutable.
	 * 
	 * @return A Set backed by this query result.
	 */
	Set<T> toUnmodifiableSet();

	/**
	 * Returns a sequential {@code Stream} of the collected objects.
	 * 
	 * @implSpec The default implementation creates a sequential {@code Stream} from
	 *           this query-results {@code Spliterator}. Implementations backed by a
	 *           {@code Collection} should override this method and call
	 *           {@link Collection#stream()}.
	 * @since 2.8
	 */
	default Stream<T> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

}
