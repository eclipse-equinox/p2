/*******************************************************************************
 * Copyright (c) 2009, 2018 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.query;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.eclipse.equinox.internal.p2.metadata.expression.Expression;
import org.eclipse.equinox.internal.p2.metadata.expression.ExpressionFactory;
import org.eclipse.equinox.internal.p2.metadata.expression.MatchExpression;
import org.eclipse.equinox.internal.p2.metadata.expression.QueryResult;
import org.eclipse.equinox.internal.p2.metadata.expression.RepeatableIterator;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.metadata.index.IIndex;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.metadata.index.IQueryWithIndex;

/**
 * A query that matches candidates against an expression.
 * @since 2.0
 */
public class ExpressionMatchQuery<T> implements IMatchQuery<T>, IQueryWithIndex<T> {
	private final IMatchExpression<T> expression;
	private final Class<? extends T> matchingClass;
	private final IEvaluationContext context;
	private final List<String> indexedMembers;

	public ExpressionMatchQuery(Class<? extends T> matchingClass, IExpression expression, Object... parameters) {
		this.matchingClass = matchingClass;
		this.expression = ExpressionUtil.getFactory().matchExpression(expression, parameters);
		this.context = this.expression.createContext();
		this.indexedMembers = Expression.getIndexCandidateMembers(matchingClass, ExpressionFactory.THIS, (Expression) expression);
	}

	public ExpressionMatchQuery(Class<? extends T> matchingClass, String expression, Object... parameters) {
		this(matchingClass, ExpressionUtil.parse(expression), parameters);
	}

	public IEvaluationContext getContext() {
		return context;
	}

	public Class<? extends T> getMatchingClass() {
		return matchingClass;
	}

	@Override
	public IQueryResult<T> perform(IIndexProvider<T> indexProvider) {
		if (((MatchExpression<T>) expression).operand == ExpressionUtil.TRUE_EXPRESSION)
			return new QueryResult<>(RepeatableIterator.create(indexProvider));
		Iterator<T> iterator = null;
		int top = indexedMembers.size();
		for (int idx = 0; idx < top; ++idx) {
			IIndex<T> index = indexProvider.getIndex(indexedMembers.get(idx));
			if (index != null) {
				iterator = index.getCandidates(context, ExpressionFactory.THIS, expression);
				if (iterator != null)
					break;
			}
		}
		if (iterator == null)
			iterator = RepeatableIterator.create(indexProvider);
		context.setIndexProvider(indexProvider);
		return perform(iterator);
	}

	@Override
	public IQueryResult<T> perform(Iterator<T> iterator) {
		if (((MatchExpression<T>) expression).operand == ExpressionUtil.TRUE_EXPRESSION)
			return new QueryResult<>(iterator);

		HashSet<T> result = null;
		while (iterator.hasNext()) {
			T value = iterator.next();
			if (isMatch(value)) {
				if (result == null)
					result = new HashSet<>();
				result.add(value);
			}
		}
		return result == null ? Collector.emptyCollector() : new CollectionResult<>(result);
	}

	@Override
	public boolean isMatch(T candidate) {
		if (!matchingClass.isInstance(candidate))
			return false;
		ExpressionFactory.THIS.setValue(context, candidate);
		return Boolean.TRUE == expression.evaluate(context);
	}

	@Override
	public IMatchExpression<T> getExpression() {
		return expression;
	}

	public void setIndexProvider(IIndexProvider<T> indexProvider) {
		context.setIndexProvider(indexProvider);
	}

	public void prePerform() { //
	}

	public void postPerform() { //
	}
}
