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
import org.eclipse.equinox.internal.p2.ql.expression.QLUtil;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.metadata.index.IQueryWithIndex;
import org.eclipse.equinox.p2.query.IQueryResult;

/**
 * An IQuery 'context query' implementation that is based on the p2 query language.
 */
public class QLContextQuery<T> extends QLQuery<T> implements IQueryWithIndex<T> {
	private final IContextExpression<T> expression;

	/**
	 * Creates a new query instance with indexed parameters.
	 * @param expression The expression to use for the query.
	 */
	public QLContextQuery(Class<T> elementClass, org.eclipse.equinox.p2.metadata.expression.IContextExpression<T> expression) {
		super(elementClass);
		this.expression = (IContextExpression<T>) expression;
	}

	/**
	 * Creates a new query instance with keyed parameters.
	 * @param elementClass The class used for filtering elements in 'everything' 
	 * @param expression The expression that represents the query.
	 * @param parameters Parameters to use for the query.
	 */
	public QLContextQuery(Class<T> elementClass, String expression, Object... parameters) {
		this(elementClass, ExpressionUtil.getFactory().<T> contextExpression(ExpressionUtil.getParser().parseQuery(expression), parameters));
	}

	public IQueryResult<T> perform(IIndexProvider<T> idxProvider) {
		return new QueryResult<T>(evaluate(idxProvider));
	}

	public IQueryResult<T> perform(Iterator<T> iterator) {
		return new QueryResult<T>(evaluate(iterator));
	}

	public Iterator<T> evaluate(IIndexProvider<T> idxProvider) {
		IEvaluationContext ctx;
		if (QLUtil.needsTranslationSupport(expression)) {
			IQueryContext<T> queryContext = QL.newQueryContext(idxProvider);
			ctx = expression.createContext(elementClass, idxProvider, queryContext.getTranslationSupport(getLocale()));
		} else
			ctx = expression.createContext(elementClass, idxProvider);
		Iterator<T> result = expression.iterator(ctx);
		return result;
	}

	public Iterator<T> evaluate(Iterator<T> iterator) {
		IEvaluationContext ctx;
		if (QLUtil.needsTranslationSupport(expression)) {
			IQueryContext<T> queryContext = QL.newQueryContext(iterator);
			ctx = expression.createContext(elementClass, iterator, queryContext.getTranslationSupport(getLocale()));
		} else
			ctx = expression.createContext(elementClass, iterator);
		Iterator<T> result = expression.iterator(ctx);
		return result;
	}

	public IExpression getExpression() {
		return expression;
	}
}
