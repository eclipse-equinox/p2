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

import org.eclipse.equinox.internal.p2.metadata.expression.ExpressionFactory;
import org.eclipse.equinox.internal.p2.metadata.expression.Expression.VariableFinder;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.query.ExpressionContextQuery;
import org.eclipse.equinox.p2.metadata.query.ExpressionQuery;

/**
 * A PipedQuery is a composite query in which each sub-query is executed in succession.  
 * The results from the ith sub-query are piped as input into the i+1th sub-query. The
 * query will short-circuit if any query returns an empty result set.
 * @since 2.0
 */
public abstract class PipedQuery<T> {
	/**
	 * Creates a piped query based on the two provided input queries. The full
	 * query input will be passed into the first query in the provided array. The
	 * second query will obtain as input the result of the first query.
	 * 
	 * @param query1 the first query
	 * @param query2 the second query
	 */
	@SuppressWarnings("unchecked")
	public static <E> IQuery<E> createPipe(IQuery<? extends E> query1, IQuery<? extends E> query2) {
		return createPipe(new IQuery[] {query1, query2});
	}

	/**
	 * Creates a piped query based on the provided input queries. The full
	 * query input will be passed into the first query in the provided array. Subsequent
	 * queries will obtain as input the result of execution of the previous query. 
	 * 
	 * @param queries the ordered list of queries to perform
	 */
	@SuppressWarnings("unchecked")
	public static <E> IQuery<E> createPipe(IQuery<E>[] queries) {
		IExpressionFactory factory = ExpressionUtil.getFactory();
		int idx = queries.length;
		IExpression[] expressions = new IExpression[idx];
		while (--idx >= 0) {
			IQuery<E> query = queries[idx];
			IExpression expr = query.getExpression();
			if (expr == null)
				expr = factory.toExpression(query);
			expressions[idx] = expr;
		}
		IExpression pipe = factory.pipe(expressions);
		VariableFinder finder = new VariableFinder(ExpressionFactory.EVERYTHING);
		pipe.accept(finder);
		return finder.isFound() ? new ExpressionContextQuery<E>((Class<E>) Object.class, pipe) : new ExpressionQuery<E>((Class<E>) Object.class, pipe);
	}
}
