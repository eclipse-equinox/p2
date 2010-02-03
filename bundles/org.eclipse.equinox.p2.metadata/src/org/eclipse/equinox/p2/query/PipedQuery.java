/******************************************************************************* 
* Copyright (c) 2009, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
*   IBM Corporation - ongoing development
******************************************************************************/
package org.eclipse.equinox.p2.query;

import java.util.*;

/**
 * A PipedQuery is a composite query in which each sub-query is executed in succession.  
 * The results from the ith sub-query are piped as input into the i+1th sub-query. The
 * query will short-circuit if any query returns an empty result set.
 * @since 2.0
 */
public class PipedQuery<T> implements ICompositeQuery<T> {
	protected final IQuery<T>[] queries;

	/**
	 * Creates a piped query based on the provided input queries. The full
	 * query input will be passed into the first query in the provided array. Subsequent
	 * queries will obtain as input the result of execution of the previous query. 
	 * 
	 * @param queries the ordered list of queries to perform
	 */
	public PipedQuery(IQuery<T>[] queries) {
		this.queries = queries;
	}

	/**
	 * Creates a piped query based on the two provided input queries. The full
	 * query input will be passed into the first query in the provided array. The
	 * second query will obtain as input the result of the first query.
	 * 
	 * @param query1 the first query
	 * @param query2 the second query
	 */
	@SuppressWarnings("unchecked")
	public PipedQuery(IQuery<T> query1, IQuery<T> query2) {
		this(new IQuery[] {query1, query2});
	}

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.p2.query.ICompositeQuery#getQueries()
	 */
	public List<IQuery<T>> getQueries() {
		return Arrays.asList(queries);
	}

	/*(non-Javadoc)
	 * @see org.eclipse.equinox.p2.query.IQuery#perform(java.util.Iterator)
	 */
	public IQueryResult<T> perform(Iterator<T> iterator) {
		IQueryResult<T> last = Collector.emptyCollector();
		if (queries.length > 0) {
			last = queries[0].perform(iterator);
			for (int i = 1; i < queries.length; i++) {
				if (last.isEmpty())
					break;
				// Take the results of the previous query and use them
				// to drive the next one (i.e. composing queries)
				last = queries[i].perform(last.iterator());
			}
		}
		return last;
	}
}
