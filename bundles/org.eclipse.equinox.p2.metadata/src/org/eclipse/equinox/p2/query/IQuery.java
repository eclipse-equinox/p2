/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.query;

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.expression.IExpression;

/**
 * The root interface for all queries that can be performed on an {@link IQueryable}.
 * A query is a piece of logic that selects some objects from a list of provided
 * inputs using some established criteria.
 * <p>
 * Any given query must be stable - running the same query on the same inputs
 * must return an equal query result each time the query is executed. Thus a client
 * that has performed a query can freely cache the result as long as they know the
 * query input has not changed.
 * </p>
 * 
 * @param <T> The type of input object that this query accepts
 * @noimplement This interface is not intended to be implemented directly by clients.
 * @noextend This interface is not intended to be extended directly by clients.
 * @since 2.0
 */
public interface IQuery<T> {
	/**
	 * Evaluates the query for a specific input.  
	 * 
	 * @param iterator The elements for which to evaluate the query on
	 * @return The results of the query.
	 */
	IQueryResult<T> perform(Iterator<T> iterator);

	/**
	 * Returns the IExpression backing this query or <code>null</code> if
	 * this is not an expression query.
	 * @return An expression or <code>null</code>.
	 */
	IExpression getExpression();
}
