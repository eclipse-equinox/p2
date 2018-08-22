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

import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IExpressionVisitor;

/**
 * A function that executes some code
 */
public class LambdaExpression extends Unary {
	protected final Variable each;

	protected LambdaExpression(Variable each, Expression body) {
		super(body);
		this.each = each;
	}

	@Override
	public boolean accept(IExpressionVisitor visitor) {
		return super.accept(visitor) && each.accept(visitor);
	}

	@Override
	public int compareTo(Expression e) {
		int cmp = super.compareTo(e);
		if (cmp == 0)
			cmp = each.compareTo(((LambdaExpression) e).each);
		return cmp;
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o) && each.equals(((LambdaExpression) o).each);
	}

	@Override
	public int hashCode() {
		int result = 31 + operand.hashCode();
		return 31 * result + each.hashCode();
	}

	@Override
	public int getExpressionType() {
		return TYPE_LAMBDA;
	}

	@Override
	public void toString(StringBuffer bld, Variable rootVariable) {
		each.toString(bld, rootVariable);
		bld.append(" | "); //$NON-NLS-1$
		appendOperand(bld, rootVariable, operand, IExpressionConstants.PRIORITY_COMMA);
	}

	public Variable getItemVariable() {
		return each;
	}

	@Override
	public String getOperator() {
		return "|"; //$NON-NLS-1$
	}

	@Override
	public int getPriority() {
		return IExpressionConstants.PRIORITY_LAMBDA;
	}

	public IEvaluationContext prolog(IEvaluationContext context) {
		return EvaluationContext.create(context, each);
	}

	@Override
	int countAccessToEverything() {
		return 2 * super.countAccessToEverything();
	}
}
