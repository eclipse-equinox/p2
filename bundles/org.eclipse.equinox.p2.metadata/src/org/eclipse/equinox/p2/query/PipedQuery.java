/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.query;

import java.util.*;
import org.eclipse.equinox.internal.p2.query.QueryHelpers;

/**
 * A PipedQuery is an aggregate query in which each sub-query
 * is executed in succession.  The results from the ith sub-query
 * are piped as input into the i+1th sub-query.
 * @since 2.0
 */
public class PipedQuery<T> implements ICompositeQuery<T> {
	protected final IQuery<T>[] queries;

	public PipedQuery(IQuery<T>[] queries) {
		this.queries = queries;
	}

	@SuppressWarnings("unchecked")
	public PipedQuery(IQuery<T> query1, IQuery<T> query2) {
		this(new IQuery[] {query1, query2});
	}

	/**
	 * Gets the ID for this Query. 
	 */
	public String getId() {
		return QueryHelpers.getId(this);
	}

	/**
	 * Gets a particular property of the query.
	 * @param property The property to retrieve 
	 */
	public Object getProperty(String property) {
		return QueryHelpers.getProperty(this, property);
	}

	/**
	 * Returns the queries that make up this compound query
	 */
	public List<IQuery<T>> getQueries() {
		return Arrays.asList(queries);
	}

	public IQueryResult<T> perform(Iterator<T> iterator) {
		IQueryResult<T> last = Collector.emptyCollector();
		if (queries.length > 0) {
			last = queries[0].perform(iterator);
			for (int i = 1; i < queries.length; i++)
				// Take the results of the previous query and using them
				// to drive the next one (i.e. composing queries)
				last = queries[i].perform(last.iterator());
		}
		return last;
	}
}
