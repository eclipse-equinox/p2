/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.query;

import org.eclipse.equinox.internal.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.query.ExpressionContextQuery;
import org.eclipse.equinox.p2.metadata.query.ExpressionQuery;

/**
 * A limit query can be used to limit the number of query results returned.  Once
 * the limit is reached, the query is terminated.
 * @since 2.0
 */
public class LimitQuery<T> extends ExpressionContextQuery<T> {

	private static <T> Class<? extends T> getElementClass(IQuery<T> query) {
		@SuppressWarnings("unchecked")
		Class<? extends T> elementClass = (Class<T>) Object.class;
		if (query instanceof ExpressionQuery<?>)
			elementClass = ((ExpressionQuery<T>) query).getMatchingClass();
		else if (query instanceof ExpressionContextQuery<?>)
			elementClass = ((ExpressionContextQuery<T>) query).getElementClass();
		return elementClass;
	}

	private static <T> IContextExpression<T> createExpression(IQuery<T> query, int limit) {
		IExpressionFactory factory = ExpressionUtil.getFactory();
		IExpression expr = query.getExpression();
		Object[] parameters;
		if (expr == null) {
			expr = factory.toExpression(query);
			if (query instanceof IMatchQuery<?>)
				expr = factory.select(ExpressionFactory.EVERYTHING, factory.lambda(ExpressionFactory.THIS, expr));
			parameters = new Object[0];
		} else {
			if (expr instanceof MatchExpression<?>) {
				MatchExpression<?> matchExpr = (MatchExpression<?>) expr;
				parameters = matchExpr.getParameters();
				expr = factory.select(ExpressionFactory.EVERYTHING, factory.lambda(ExpressionFactory.THIS, matchExpr.operand));
			} else if (expr instanceof ContextExpression<?>) {
				ContextExpression<?> contextExpr = (ContextExpression<?>) expr;
				parameters = contextExpr.getParameters();
				expr = contextExpr.operand;
			} else
				parameters = new Object[0];
		}
		return factory.contextExpression(factory.limit(expr, limit), parameters);
	}

	public LimitQuery(IQuery<T> query, int limit) {
		super(getElementClass(query), createExpression(query, limit));
	}
}
