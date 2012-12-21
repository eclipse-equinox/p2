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

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.expression.IExpression;

/**
 * This class represents a simple Java-based query. Clients may subclass and
 * override the {@link #isMatch(Object)} method in order to write simple
 * queries against p2 metadata.
 * <p>
 * Note that hand-written queries cannot be optimized for queryables containing
 * indices, or for remote queryables. In general you should use one of the pre-defined
 * queries found in {@link QueryUtil} if possible, to obtain queries optimized for indexing and
 * remote execution. This class is intended for simple queries against small data
 * sources where indexed lookup and remote query execution are not needed.
 * </p>
 * @deprecated If possible, use one of the predefined queries in {@link QueryUtil} 
 * or use the {@link QueryUtil#createMatchQuery(String, Object...)}
 * to create a custom expression based query. If the query cannot be expressed using
 * the p2QL, then use a predefined or custom expression query as a first filter
 * (in worst case, use {@link QueryUtil#createIUAnyQuery()}) and then provide further filtering
 * like so:<pre>
 * for(iter = queryable.query(someExpressionQuery).iterator(); iter.hasNext();) {
 *   // do your match here
 * }</pre>
 * @since 2.0
 */
public abstract class MatchQuery<T> implements IMatchQuery<T> {

	/**
	 * Returns whether the given object satisfies the parameters of this query.
	 * 
	 * @param candidate The object to perform the query against
	 * @return <code>true</code> if the unit satisfies the parameters
	 * of this query, and <code>false</code> otherwise
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 * Clients should call {@link #perform(Iterator)}
	 * @since 2.0
	 */
	public abstract boolean isMatch(T candidate);

	/**
	 * Performs this query on the given iterator, passing all objects in the iterator 
	 * that match the criteria of this query to the given result.
	 */
	public final IQueryResult<T> perform(Iterator<T> iterator) {
		Collector<T> result = new Collector<T>();
		while (iterator.hasNext()) {
			T candidate = iterator.next();
			if (candidate != null && isMatch(candidate))
				if (!result.accept(candidate))
					break;
		}
		return result;
	}

	/**
	 * Execute any pre-processing that must be done before this query is performed against
	 * a particular iterator.  This method may be used by subclasses to do any calculations,
	 * caching, or other preparation for the query.
	 * <p>
	 * This method is internal to the framework.  Subclasses may override this method, but
	 * should not call this method.
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void prePerform() {
		// nothing to do by default
	}

	/**
	 * Execute any post-processing that must be done after this query has been performed against
	 * a particular iterator.  This method may be used by subclasses to clear caches or any other
	 * cleanup that should occur after a query.  
	 * <p>
	 * This method will be called even if the query does not complete successfully.
	 * <p>
	 * This method is internal to the framework.  Subclasses may override this method, but
	 * should not call this method.
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void postPerform() {
		// nothing to do by default
	}

	public IExpression getExpression() {
		return null;
	}
}
