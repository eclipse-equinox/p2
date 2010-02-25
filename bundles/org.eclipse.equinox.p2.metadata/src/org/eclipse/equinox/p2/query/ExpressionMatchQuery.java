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

import java.util.HashSet;
import java.util.Iterator;
import org.eclipse.equinox.internal.p2.metadata.expression.Expression;
import org.eclipse.equinox.internal.p2.metadata.expression.ExpressionFactory;
import org.eclipse.equinox.internal.p2.metadata.query.IMatchQuery;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.index.*;

/**
 * A query that matches candidates against an expression.
 * @since 2.0
 */
public class ExpressionMatchQuery<T> implements IMatchQuery<T>, IQueryWithIndex<T> {
	private final IMatchExpression<T> expression;
	private final Class<? extends T> matchingClass;
	private IEvaluationContext context;

	public ExpressionMatchQuery(Class<? extends T> matchingClass, IExpression expression, Object... parameters) {
		this.matchingClass = matchingClass;
		this.expression = ExpressionUtil.getFactory().<T> matchExpression(expression, parameters);
		this.context = this.expression.createContext();
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

	public IQueryResult<T> perform(IIndexProvider<T> indexProvider) {
		Iterator<T> iterator = null;
		for (String member : Expression.getIndexCandidateMembers(IArtifactKey.class, ExpressionFactory.THIS, (Expression) expression)) {
			IIndex<T> index = indexProvider.getIndex(member);
			if (index != null) {
				iterator = index.getCandidates(context, ExpressionFactory.THIS, expression);
				if (iterator != null)
					break;
			}
		}
		if (iterator == null)
			iterator = indexProvider.everything();
		context.setIndexProvider(indexProvider);
		return perform(iterator);
	}

	public IQueryResult<T> perform(Iterator<T> iterator) {
		HashSet<T> result = null;
		while (iterator.hasNext()) {
			T value = iterator.next();
			if (isMatch(value)) {
				if (result == null)
					result = new HashSet<T>();
				result.add(value);
			}
		}
		return result == null ? Collector.<T> emptyCollector() : new CollectionResult<T>(result);
	}

	public boolean isMatch(T candidate) {
		if (!matchingClass.isInstance(candidate))
			return false;
		ExpressionFactory.THIS.setValue(context, candidate);
		return Boolean.TRUE == expression.evaluate(context);
	}

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
