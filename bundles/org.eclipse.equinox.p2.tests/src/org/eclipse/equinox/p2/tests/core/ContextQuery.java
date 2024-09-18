/*******************************************************************************
* Copyright (c) 2009, 2017 EclipseSource and others.
 *
 * This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
* available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;

/**
 * <p>
 * ContextQuery is the abstract superclass for Queries that require the entire
 * input to evaluate the results. Queries must consider the group of elements
 * before processing the results.
 * </p>
 *
 * <p>
 * ContextQueries must also be transitive. That is, if run on a subset of the
 * input, the order in which they are executed must not matter. If there is the
 * need for a non-transitive query, please see:
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=261403
 * </p>
 * <p>
 * Users of this query must call {@link #perform(Iterator)} to compute the
 * results.
 * </p>
 * <p>
 * This class may be subclassed by clients. Subclasses should specify the type
 * of object they support querying on. Subclasses are also encouraged to clearly
 * specify their match algorithm, and expose the parameters involved in the
 * match computation, to allow {@link org.eclipse.equinox.p2.query.IQueryable}
 * implementations to optimize their execution of the query.
 * </p>
 *
 * @since 2.0
 */
public abstract class ContextQuery<T> implements IQuery<T> {

	/**
	 * Evaluates the query for a specific input.
	 *
	 * @param iterator The elements for which to evaluate the query on
	 * @return The results of the query.  The collector returned must be
	 * the collector passed in.
	 */
	@Override
	public abstract IQueryResult<T> perform(Iterator<T> iterator);

	@Override
	public IExpression getExpression() {
		return null;
	}
}
