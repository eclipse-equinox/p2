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
 *     Cloudsmith Inc. - added index capabilities
 *******************************************************************************/
package org.eclipse.equinox.p2.query;

import org.eclipse.equinox.internal.p2.metadata.expression.ExpressionFactory;
import org.eclipse.equinox.internal.p2.metadata.expression.Expression.VariableFinder;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.query.ExpressionContextQuery;
import org.eclipse.equinox.p2.metadata.query.ExpressionQuery;

/**
 * A query that combines a group of sub-queries.<P>
 * 
 * In a CompoundQuery each sub-query is executed over the entire input and the 
 * results are combined using either logical AND or logical OR operations. <P>
 * 
 * Clients are expected to call {@link CompoundQuery#createCompoundQuery(IQuery[], boolean)}
 * to create a concrete instance of a CompoundQuery.  If all Queries are instances of 
 * {@link IMatchQuery} then the resulting compound query will be an {@link IMatchQuery}, otherwise the
 * resulting query will be a context query}.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public abstract class CompoundQuery<T> {
	/**
	 * Creates a compound query that combines the given queries. The queries
	 * will be performed by the compound query in the given order. This method
	 * might not perform all queries if it can determine the result of the compound
	 * expression without doing so.
	 * 
	 * If all the queries are instances of {@link IMatchQuery} then the resulting
	 * compound query will be an instance of IMatchQuery, otherwise the resulting
	 * compound query will be a context query.
	 * 
	 * @param queries The queries to perform
	 * @param and <code>true</code> if this query represents a logical 'and', and
	 * <code>false</code> if this query represents a logical 'or'.
	 */
	@SuppressWarnings("unchecked")
	public static <E> IQuery<E> createCompoundQuery(IQuery<E>[] queries, boolean and) {
		IExpressionFactory factory = ExpressionUtil.getFactory();
		int idx = queries.length;
		if (idx == 1)
			return queries[0];

		Class<? extends E> elementClass = (Class<E>) Object.class;
		if (idx == 0)
			return new ExpressionQuery<E>(elementClass, ExpressionQuery.matchAll());

		IExpression[] expressions = new IExpression[idx];
		boolean justBooleans = true;
		boolean justContexts = true;
		while (--idx >= 0) {
			IQuery<E> query = queries[idx];
			if (query instanceof IMatchQuery<?>)
				justContexts = false;
			else
				justBooleans = false;

			IExpression expr = query.getExpression();
			if (expr == null)
				expr = factory.toExpression(query);

			Class<? extends E> ec = ExpressionContextQuery.getElementClass(query);
			if (elementClass == null)
				elementClass = ec;
			else if (elementClass != ec) {
				if (elementClass.isAssignableFrom(ec)) {
					if (and)
						// Use most restrictive class
						elementClass = ec;
				} else if (ec.isAssignableFrom(elementClass)) {
					if (!and)
						// Use least restrictive class
						elementClass = ec;
				}
			}
			expressions[idx] = expr;
		}

		if (justBooleans) {
			IExpression compound = and ? factory.and(expressions) : factory.or(expressions);
			return new ExpressionQuery<E>(elementClass, compound);
		}

		if (!justContexts) {
			// Mix of boolean queries and context queries. All must be converted into context then.
			for (idx = 0; idx < expressions.length; ++idx)
				expressions[idx] = makeContextExpression(factory, expressions[idx]);
		}

		IExpression compound = expressions[0];
		for (idx = 1; idx < expressions.length; ++idx)
			compound = and ? factory.intersect(compound, expressions[idx]) : factory.union(compound, expressions[idx]);
		return new ExpressionContextQuery<E>(elementClass, compound);
	}

	@SuppressWarnings("unchecked")
	public static <T> IQuery<T> createCompoundQuery(IQuery<T> query1, IQuery<T> query2, boolean and) {
		return createCompoundQuery(new IQuery[] {query1, query2}, and);
	}

	private static IExpression makeContextExpression(IExpressionFactory factory, IExpression expr) {
		VariableFinder finder = new VariableFinder(ExpressionFactory.EVERYTHING);
		expr.accept(finder);
		if (!finder.isFound())
			expr = factory.select(ExpressionFactory.EVERYTHING, factory.lambda(ExpressionFactory.THIS, expr));
		return expr;
	}
}
