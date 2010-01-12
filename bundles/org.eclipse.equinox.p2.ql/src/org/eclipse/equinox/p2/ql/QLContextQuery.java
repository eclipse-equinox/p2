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
package org.eclipse.equinox.p2.ql;

import java.util.Iterator;
import org.eclipse.equinox.p2.query.IQueryResult;

/**
 * An IQuery 'context query' implementation that is based on the p2 query language.
 */
public class QLContextQuery<T> extends QLQuery<T> {
	private final IContextExpression<T> expression;

	/**
	 * Creates a new query instance with indexed parameters.
	 * @param expression The expression that represents the query.
	 * @param parameters Parameters to use for the query.
	 */
	public QLContextQuery(IContextExpression<T> expression, Object... parameters) {
		super(expression.getElementClass(), parameters);
		this.expression = expression;
	}

	/**
	 * Creates a new query instance with keyed parameters.
	 * @param elementClass The class used for filtering elements in 'everything' 
	 * @param expression The expression that represents the query.
	 * @param parameters Parameters to use for the query.
	 */
	public QLContextQuery(Class<T> elementClass, String expression, Object... parameters) {
		this(parser.parseQuery(elementClass, expression), parameters);
	}

	public IQueryResult<T> perform(Iterator<T> iterator) {
		return new QueryResult<T>(evaluate(iterator));
	}

	public Iterator<T> evaluate(Iterator<T> iterator) {
		IEvaluationContext ctx;
		if (expression.needsTranslations()) {
			IQueryContext<T> queryContext = QL.newQueryContext(iterator);
			ctx = expression.createContext(iterator, parameters, queryContext.getTranslationSupport(getLocale()));
		} else
			ctx = expression.createContext(iterator, parameters);
		@SuppressWarnings("unchecked")
		Iterator<T> result = (Iterator<T>) expression.evaluateAsIterator(ctx);
		return result;
	}

	/**
	 * Query without using a collector. Instead, return the result of the query directly.
	 * @param queryContext The context for the query.
	 * @return The result of the query.
	 */
	public Object query(IQueryContext<T> queryContext) {
		// Check if we need translation support
		//
		IEvaluationContext ctx;
		if (expression.needsTranslations())
			ctx = expression.createContext(queryContext.iterator(), parameters, queryContext.getTranslationSupport(getLocale()));
		else
			ctx = expression.createContext(queryContext.iterator(), parameters);
		return expression.evaluate(ctx);
	}
}
