/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
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
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.util.Arrays;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.p2.metadata.expression.*;

/**
 * The MatchExpression is a wrapper for an {@link IExpression} that is expected
 * to return a boolean value. The wrapper provides the evaluation context needed
 * to evaluate the expression.
 */
public class MatchExpression<T> extends Unary implements IMatchExpression<T> {
	private static final Object[] noParams = new Object[0];
	private final Object[] parameters;

	MatchExpression(Expression expression, Object[] parameters) {
		super(expression);
		this.parameters = parameters == null ? noParams : parameters;
	}

	@Override
	public boolean accept(IExpressionVisitor visitor) {
		return operand.accept(visitor);
	}

	@Override
	public IEvaluationContext createContext() {
		return EvaluationContext.create(parameters, ExpressionFactory.THIS);
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o) && Arrays.equals(parameters, ((MatchExpression<?>) o).parameters);
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		return operand.evaluate(parameters.length == 0 ? context : EvaluationContext.create(context, parameters));
	}

	@Override
	public int getExpressionType() {
		return 0;
	}

	@Override
	public String getOperator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] getParameters() {
		return parameters;
	}

	/**
	 * Returns the predicate expression that is used for the match
	 * @return The predicate expression
	 */
	IExpression getPredicate() {
		return operand;
	}

	@Override
	public int getPriority() {
		return operand.getPriority();
	}

	@Override
	public int hashCode() {
		return operand.hashCode() * 31 + CollectionUtils.hashCode(parameters);
	}

	@Override
	public boolean isMatch(IEvaluationContext context, T value) {
		ExpressionFactory.THIS.setValue(context, value);
		return Boolean.TRUE == operand.evaluate(context);
	}

	@Override
	public boolean isMatch(T value) {
		return isMatch(createContext(), value);
	}

	@Override
	public void toLDAPString(StringBuffer bld) {
		operand.toLDAPString(bld);
	}

	@Override
	public void toString(StringBuffer bld, Variable rootVariable) {
		operand.toString(bld, rootVariable);
	}
}
