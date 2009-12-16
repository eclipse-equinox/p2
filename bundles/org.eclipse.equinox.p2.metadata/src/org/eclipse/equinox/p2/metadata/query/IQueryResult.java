/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.metadata.query;

import java.util.Collection;
import java.util.Iterator;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;

/**
 * An IQueryResult represents the results of a query.  
 * @since 2.0
 *
 */
public interface IQueryResult extends IQueryable {

	/**
	 * Adds the elements from one QueryResult to this one
	 * @param queryResult A QueryResult from which the elements should be retrieved
	 */
	public void addAll(IQueryResult queryResult);

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
	public Iterator iterator();

	/**
	 * Returns the number of collected objects.
	 */
	public int size();

	/**
	 * Returns the collected objects as an array
	 * 
	 * @param clazz The type of array to return
	 * @return The array of results
	 * @throws ArrayStoreException the runtime type of the specified array is
	 *         not a supertype of the runtime type of every collected object
	 */
	public Object[] toArray(Class clazz);

	/**
	 * Returns the collected objects as an immutable collection.
	 * 
	 * @return An unmodifiable collection of the collected objects
	 */
	public Collection toCollection();

}
