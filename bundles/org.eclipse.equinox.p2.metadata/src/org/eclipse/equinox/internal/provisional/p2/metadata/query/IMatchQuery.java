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

import org.eclipse.equinox.p2.metadata.query.IQuery;

/**
 * A query in which the elements can be evaluated by calling isMatch on. Each
 * element can be evaluated independently of all other elements.  Match queries
 * can be evaluated in parallel as each call {@link #isMatch(Object)} is mutually
 * exclusive from all other calls. <P>
 * 
 * @spi Clients should not implement this interface, but rather extend {@link MatchQuery}.
 */
public interface IMatchQuery<T> extends IQuery<T> {

	/**
	 * Returns whether the given object satisfies the parameters of this query.
	 * 
	 * @param candidate The object to perform the query against
	 * @return <code>true</code> if the unit satisfies the parameters
	 * of this query, and <code>false</code> otherwise
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public boolean isMatch(T candidate);

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
	public void prePerform();

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
	public void postPerform();
}
