/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
*   IBM Corporation - ongoing development
******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.query;

import org.eclipse.equinox.p2.query.ExpressionMatchQuery;

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.query.*;

/**
 * This class represents the superclass of most of p2's queries.  Every element
 * in the query can be evaluated by calling isMatch on it. If {@link #isMatch(Object)} returns true, 
 * then the element WILL be included in the query result.  If {@link #isMatch(Object)} returns false, then 
 * the element WILL NOT be included in the query result.
 * <p>
 * This class may be subclassed by clients. Subclasses should specify the type
 * of object they support querying on. Subclasses are also encouraged to clearly
 * specify their match algorithm, and expose the parameters involved in the match
 * computation, to allow {@link IQueryable} implementations to optimize their
 * execution of the query. 
 * @since 2.0
 * @deprecated Clients should use {@link ExpressionMatchQuery} instead.
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
