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

import java.util.*;
import org.eclipse.core.runtime.*;

/**
 * A queryable that holds a number of other IQueryables and provides
 * a mechanism for querying the entire set.
 */
public class CompoundQueryable implements IQueryable {

	private IQueryable[] queryables;

	public CompoundQueryable(IQueryable[] queryables) {
		this.queryables = queryables;
	}

	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		boolean isMatchQuery = query instanceof IMatchQuery;
		Collector results = collector;
		int totalWork = isMatchQuery ? queryables.length : queryables.length + 1;

		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor, totalWork * 10);
			if (!isMatchQuery) {
				// If it is not a match query, then collect the results
				// as a list, we will query this list for the final results
				results = new ListCollector();
			}
			for (int i = 0; i < queryables.length; i++) {
				if (subMonitor.isCanceled())
					break;
				results = queryables[i].query(query, results, subMonitor.newChild(10));
			}

			if (!isMatchQuery) {
				// If it is not a MatchQuery then we must query the results.
				collector = results.query(query, collector, subMonitor.newChild(10));
			} else
				collector = results;
		} finally {
			monitor.done();
		}

		return collector;
	}

	/**
	 * A list collector.
	 * 
	 * This is a collector backed as a list.
	 *
	 */
	private class ListCollector extends Collector {
		private List collected;

		public ListCollector() {
			super();
		}

		public boolean accept(Object object) {
			if (collected == null)
				collected = new ArrayList();
			collected.add(object);
			return true;
		}

		/**
		 * Returns the collected objects as an immutable collection.
		 * 
		 * @return An unmodifiable collection of the collected objects
		 */
		public Collection toCollection() {
			return collected == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(collected);
		}

		public Iterator iterator() {
			return collected == null ? Collections.EMPTY_LIST.iterator() : collected.iterator();
		}

		public int size() {
			return collected == null ? 0 : collected.size();
		}
	}
}
