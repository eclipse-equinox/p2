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

import java.lang.reflect.Array;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.metadata.query.IQuery;

/**
 * A queryable that holds a number of other IQueryables and provides
 * a mechanism for querying the entire set.
 */
public class CompoundQueryable implements IQueryable {

	private IQueryable[] queryables;

	public CompoundQueryable(IQueryable[] queryables) {
		this.queryables = queryables;
	}

	public Collector query(IQuery query, IProgressMonitor monitor) {
		Collector results = new Collector();
		Collector subResults = null;
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		boolean isMatchQuery = query instanceof IMatchQuery;
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
				subResults = queryables[i].query(query, subMonitor.newChild(10));
				results.addAll(subResults);
			}

			if (!isMatchQuery) {
				// If it is not a MatchQuery then we must query the results.
				results = results.query(query, subMonitor.newChild(10));
			} else
				results = results;
		} finally {
			monitor.done();
		}

		return results;
	}

	/**
	 * A list collector.
	 * 
	 * This is a collector backed as a list.
	 * 
	 * The list collector is not intended to be used outside of this class.  It is only public
	 * for testing purposes.
	 * 
	 * @noinstantiate This class is not intended to be instantiated by clients.
	 * @noextend This class is not intended to be subclassed by clients.
	 * 
	 */
	public class ListCollector extends Collector {
		private List collected;

		public ListCollector() {
			super();
		}

		protected Collection getCollection() {
			if (collected == null)
				collected = new ArrayList();
			return collected;
		}

		public boolean isEmpty() {
			return collected == null || collected.isEmpty();
		}

		public Object[] toArray(Class clazz) {
			int size = collected == null ? 0 : collected.size();
			Object[] result = (Object[]) Array.newInstance(clazz, size);
			if (size != 0)
				collected.toArray(result);
			return result;
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
