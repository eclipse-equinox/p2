/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.query;

import org.eclipse.equinox.internal.p2.metadata.expression.ExpressionFactory;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.query.MatchQuery;

/**
 * A query that matches candidates against an expression.
 */
public class ExpressionQuery<T> extends MatchQuery<T> {
	private final IMatchExpression<T> expression;
	private final IEvaluationContext context;
	private final Class<T> matchingClass;

	public ExpressionQuery(Class<T> matchingClass, IExpression expression, Object... parameters) {
		this(matchingClass, ExpressionUtil.getFactory().<T> matchExpression(expression, parameters));
	}

	public ExpressionQuery(Class<T> matchingClass, IMatchExpression<T> expression) {
		this.matchingClass = matchingClass;
		this.expression = expression;
		this.context = expression.createContext();
	}

	@Override
	public boolean isMatch(T candidate) {
		if (!matchingClass.isInstance(candidate))
			return false;
		ExpressionFactory.THIS.setValue(context, candidate);
		return Boolean.TRUE == expression.evaluate(context);
	}

	public IMatchExpression<T> getExpression() {
		return expression;
	}
}
