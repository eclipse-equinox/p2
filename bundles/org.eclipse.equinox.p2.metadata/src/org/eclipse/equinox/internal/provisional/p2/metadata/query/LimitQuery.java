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

import java.util.*;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;

/**
 * A limit query can be used to limit the number of query results returned.  Once
 * the limit is reached, the query is terminated.
 */
public class LimitQuery<T> extends ContextQuery<T> implements ICompositeQuery<T> {

	private final IQuery<T> query;
	private final int limit;

	public LimitQuery(IQuery<T> query, int limit) {
		this.query = query;
		this.limit = limit;
	}

	public IQueryResult<T> perform(Iterator<T> iterator) {
		if (limit == 0)
			return Collector.emptyCollector();

		int count = 0;
		Collector<T> result = new Collector<T>();
		if (query instanceof IMatchQuery<?>) {
			IMatchQuery<T> matchQuery = (IMatchQuery<T>) query;
			while (iterator.hasNext()) {
				T candidate = iterator.next();
				if (matchQuery.isMatch(candidate)) {
					result.accept(candidate);
					if (++count >= limit)
						break;
				}
			}
		} else {
			iterator = query.perform(iterator).iterator();
			while (++count <= limit && iterator.hasNext())
				result.accept(iterator.next());
		}
		return result;
	}

	public List<IQuery<T>> getQueries() {
		return Collections.singletonList(query);
	}

}
