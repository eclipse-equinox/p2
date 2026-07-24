/*******************************************************************************
 * Copyright (c) 2009, 2026 Cloudsmith Inc. and others.
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
	public void toLDAPString(StringBuilder bld) {
		if (parameters.length == 0) {
			operand.toLDAPString(bld);
		} else {
			substituteParameters(operand).toLDAPString(bld);
		}
	}

	/**
	 * Returns a copy of the expression tree with every {@link Parameter} node
	 * replaced by a {@link Literal} wrapping its bound value from
	 * {@link #parameters}. Once substituted, the existing {@code toLDAPString}
	 * implementations on {@link Equals} and {@link Compare} (which already handle
	 * {@code Literal} rhs values) can serialise the tree without any further
	 * changes.
	 */
	private Expression substituteParameters(Expression expr) {
		if (expr instanceof Parameter p) {
			return Literal.create(parameters[p.position]);
		}
		if (expr instanceof NAry nary) {
			Expression[] ops = nary.operands;
			Expression[] resolved = new Expression[ops.length];
			boolean changed = false;
			for (int i = 0; i < ops.length; i++) {
				resolved[i] = substituteParameters(ops[i]);
				if (resolved[i] != ops[i]) {
					changed = true;
				}
			}
			if (!changed) {
				return expr;
			}
			if (expr instanceof And) {
				return new And(resolved);
			}
			if (expr instanceof Or) {
				return new Or(resolved);
			}
		}
		if (expr instanceof Binary b) {
			Expression resolvedLhs = substituteParameters(b.lhs);
			Expression resolvedRhs = substituteParameters(b.rhs);
			if (resolvedLhs == b.lhs && resolvedRhs == b.rhs) {
				return expr;
			}
			if (expr instanceof Equals equals) {
				return new Equals(resolvedLhs, resolvedRhs, equals.negate);
			}
			if (expr instanceof Compare compare) {
				return new Compare(resolvedLhs, resolvedRhs, compare.compareLess, compare.equalOK);
			}
		}
		if (expr instanceof CollectionFilter collectionFilter) {
			Expression resolvedCollection = substituteParameters(collectionFilter.operand);
			LambdaExpression resolvedLambda = (LambdaExpression) substituteParameters(collectionFilter.lambda);
			if (resolvedCollection == collectionFilter.operand && resolvedLambda == collectionFilter.lambda) {
				return expr;
			}
			if (expr instanceof Exists) {
				return new Exists(resolvedCollection, resolvedLambda);
			}
		}
		if (expr instanceof LambdaExpression lambda) {
			Expression resolvedBody = substituteParameters(lambda.operand);
			if (resolvedBody == lambda.operand) {
				return expr;
			}
			return new LambdaExpression(lambda.each, resolvedBody);
		}
		return expr;
	}

	@Override
	public void toString(StringBuilder bld, Variable rootVariable) {
		operand.toString(bld, rootVariable);
	}
}
