/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.query;

import java.util.Iterator;

/**
 * A Composite Query is an aggregate query in which each sub-query
 * is executed in succession.  The results from the ith sub-query
 * are piped as input into the i+1th sub-query.
 */
public class CompositeQuery implements Query {
	protected Query[] queries;

	public CompositeQuery(Query[] queries) {
		this.queries = queries;
	}

	public Collector perform(Iterator iterator, Collector result) {
		Collector collector;
		Iterator iter = iterator;
		for (int i = 0; i < queries.length; i++) {
			// Take the results of the previous query and using them
			// to drive the next one (i.e. composing queries)
			collector = queries[i].perform(iter, new Collector());
			iter = collector.iterator();
		}
		boolean gatherResults = true;
		while (iter.hasNext() && gatherResults)
			gatherResults = result.accept(iter.next());
		return result;
	}

}
