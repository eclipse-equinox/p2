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

import java.util.List;

/**
 * A query that contains a number of sub queries.  All queries that support sub-queries 
 * should implement this interface so clients can access the sub queries.
 * @since 2.0
 */
public interface ICompositeQuery<T> extends IQuery<T> {

	/**
	 * Returns all the child queries of a CompositeQuery.
	 * @return All the child queries.
	 */
	public List<IQuery<T>> getQueries();

}
