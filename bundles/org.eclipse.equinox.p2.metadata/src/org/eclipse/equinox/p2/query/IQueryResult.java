/******************************************************************************* 
* Copyright (c) 2009, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.query;

import java.util.Iterator;
import java.util.Set;

/**
 * An IQueryResult represents the results of a query.  
 * 
 * @since 2.0
 */
public interface IQueryResult<T> extends IQueryable<T>, Iterable<T> {
	/**
	 * Returns whether this QueryResult  is empty.
	 * @return <code>true</code> if this QueryResult has accepted any results,
	 * and <code>false</code> otherwise.
	 */
	public boolean isEmpty();

	/**
	 * Returns an iterator on the collected objects.
	 * 
	 * @return an iterator of the collected objects.
	 */
	public Iterator<T> iterator();

	/**
	 * Returns the collected objects as an array
	 * 
	 * @param clazz The type of array to return
	 * @return The array of results
	 * @throws ArrayStoreException the runtime type of the specified array is
	 *         not a super-type of the runtime type of every collected object
	 */
	public T[] toArray(Class<T> clazz);

	/**
	 * Creates a new Set copy with the contents of this query result. The
	 * copy can be altered without any side effects on its origin.
	 * @return A detached copy of the result.
	 */
	public Set<T> toSet();

	/**
	 * Returns a Set backed by this query result. The set is immutable.
	 * @return A Set backed by this query result.
	 */
	public Set<T> toUnmodifiableSet();
}
