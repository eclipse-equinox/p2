/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata.query;

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;

/**
 * A PipedQuery is an aggregate query in which each sub-query
 * is executed in succession.  The results from the ith sub-query
 * are piped as input into the i+1th sub-query.
 */
public class PipedQuery implements IQuery, ICompositeQuery {
	protected IQuery[] queries;

	public PipedQuery(IQuery[] queries) {
		this.queries = queries;
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
	 * Set the queries of this composite.  This is needed to allow subclasses of 
	 * CompsiteQuery to set the queries in a constructor
	 */
	protected final void setQueries(IQuery[] queries) {
		this.queries = queries;
	}

	/**
	 * Returns the queries that make up this compound query
	 */
	public IQuery[] getQueries() {
		return queries;
	}

	public IQueryResult perform(Iterator iterator) {
		IQueryResult last = Collector.EMPTY_COLLECTOR;
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
