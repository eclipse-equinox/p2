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

/**
 * A limit query can be used to limit the number of query results returned.  Once
 * the limit is reached, the query is terminated.
 */
public class LimitQuery extends ContextQuery implements ICompositeQuery {

	private final IQuery query;
	private final int limit;

	private class LimitCollector extends Collector {
		private final Collector subCollector;
		private final int collectionLimit;
		private int collected = 0;

		public LimitCollector(int collectionLimit, Collector subCollector) {
			this.collectionLimit = collectionLimit;
			this.subCollector = subCollector;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector#accept(java.lang.Object)
		 */
		public boolean accept(Object object) {
			if (collected >= collectionLimit)
				return false;
			if (!subCollector.accept(object))
				return false;
			collected++;
			if (collected >= collectionLimit)
				return false;
			return true;
		}
	}

	public LimitQuery(IQuery query, int limit) {
		this.query = query;
		this.limit = limit;
	}

	public Collector perform(Iterator iterator, Collector result) {
		LimitCollector limitCollector = new LimitCollector(limit, result);
		query.perform(iterator, limitCollector);
		return result;
	}

	public IQuery[] getQueries() {
		return new IQuery[] {query};
	}

}
