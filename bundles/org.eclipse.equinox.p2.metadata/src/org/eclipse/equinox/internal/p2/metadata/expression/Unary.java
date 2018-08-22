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
 * The abstract base class for all unary expressions
 */
public abstract class Unary extends Expression {
	public final Expression operand;

	protected Unary(Expression operand) {
		this.operand = operand;
	}

	@Override
	public boolean accept(IExpressionVisitor visitor) {
		return super.accept(visitor) && operand.accept(visitor);
	}

	@Override
	public int compareTo(Expression e) {
		int cmp = super.compareTo(e);
		if (cmp == 0)
			cmp = operand.compareTo(((Unary) e).operand);
		return cmp;
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o) && operand.equals(((Unary) o).operand);
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		return operand.evaluate(context);
	}

	@Override
	public int hashCode() {
		return operand.hashCode() * 3 + operand.getExpressionType();
	}

	public Expression getOperand() {
		return operand;
	}

	@Override
	public void toString(StringBuffer bld, Variable rootVariable) {
		bld.append(getOperator());
		appendOperand(bld, rootVariable, operand, getPriority());
	}

	@Override
	int countAccessToEverything() {
		return operand.countAccessToEverything();
	}
}