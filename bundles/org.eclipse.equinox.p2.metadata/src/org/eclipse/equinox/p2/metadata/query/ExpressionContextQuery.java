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
package org.eclipse.equinox.p2.metadata.query;

import java.util.Iterator;
import org.eclipse.equinox.internal.p2.metadata.expression.QueryResult;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.metadata.index.IQueryWithIndex;
import org.eclipse.equinox.p2.query.ContextQuery;
import org.eclipse.equinox.p2.query.IQueryResult;

/**
 * A query that evaluates using an iterator as input and produces a new iterator.
 * @since 2.0
 */
public class ExpressionContextQuery<T> extends ContextQuery<T> implements IQueryWithIndex<T> {
	public static final IMatchExpression<IInstallableUnit> MATCH_ALL_UNITS = ExpressionUtil.getFactory().matchExpression(ExpressionUtil.TRUE_EXPRESSION);
	public static final IMatchExpression<IInstallableUnit> MATCH_NO_UNIT = ExpressionUtil.getFactory().matchExpression(ExpressionUtil.FALSE_EXPRESSION);

	private final IContextExpression<T> expression;
	private final Class<? extends T> elementClass;

	public ExpressionContextQuery(Class<? extends T> elementClass, IExpression expression, Object... parameters) {
		this(elementClass, ExpressionUtil.getFactory().<T> contextExpression(expression, parameters));
	}

	public ExpressionContextQuery(Class<? extends T> elementClass, IContextExpression<T> expression) {
		this.elementClass = elementClass;
		this.expression = expression;
	}

	public ExpressionContextQuery(Class<? extends T> matchingClass, String expression, Object... parameters) {
		this(matchingClass, ExpressionUtil.getFactory().<T> contextExpression(ExpressionUtil.getParser().parseQuery(expression), parameters));
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
}
