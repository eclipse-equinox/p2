/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.query;


import java.util.Iterator;
import org.eclipse.equinox.internal.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.metadata.index.IQueryWithIndex;

/**
 * A query that evaluates using an iterator as input and produces a new iterator.
 * @since 2.0
 */
public class ExpressionQuery<T> implements IQueryWithIndex<T> {
	private final IContextExpression<T> expression;
	private final Class<? extends T> elementClass;

	public ExpressionQuery(Class<? extends T> elementClass, IExpression expression, Object... parameters) {
		this.elementClass = elementClass;
		this.expression = ExpressionUtil.getFactory().<T> contextExpression(expression, parameters);
	}

	public ExpressionQuery(Class<? extends T> matchingClass, String expression, Object... parameters) {
		this(matchingClass, ExpressionUtil.parseQuery(expression), parameters);
	}

	public Class<? extends T> getElementClass() {
		return elementClass;
	}

	public IQueryResult<T> perform(IIndexProvider<T> indexProvider) {
		return new QueryResult<T>(expression.iterator(expression.createContext(elementClass, indexProvider)));
	}

	public IQueryResult<T> perform(Iterator<T> iterator) {
		return new QueryResult<T>(expression.iterator(expression.createContext(elementClass, iterator)));
	}

	public IContextExpression<T> getExpression() {
		return expression;
	}

	public static <T> Class<? extends T> getElementClass(IQuery<T> query) {
		@SuppressWarnings("unchecked")
		Class<? extends T> elementClass = (Class<T>) Object.class;
		if (query instanceof ExpressionMatchQuery<?>)
			elementClass = ((ExpressionMatchQuery<T>) query).getMatchingClass();
		else if (query instanceof ExpressionQuery<?>)
			elementClass = ((ExpressionQuery<T>) query).getElementClass();
		return elementClass;
	}

	public static <T> IContextExpression<T> createExpression(IQuery<T> query) {
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
		return factory.contextExpression(expr, parameters);
	}

}
