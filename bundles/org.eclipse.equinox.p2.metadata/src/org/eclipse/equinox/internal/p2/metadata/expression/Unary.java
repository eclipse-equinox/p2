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

	public boolean accept(IExpressionVisitor visitor) {
		return super.accept(visitor) && operand.accept(visitor);
	}

	public int compareTo(Expression e) {
		int cmp = super.compareTo(e);
		if (cmp == 0)
			cmp = operand.compareTo(((Unary) e).operand);
		return cmp;
	}

	public boolean equals(Object o) {
		return super.equals(o) && operand.equals(((Unary) o).operand);
	}

	public Object evaluate(IEvaluationContext context) {
		return operand.evaluate(context);
	}

	public int hashCode() {
		return operand.hashCode() * 3 + operand.getExpressionType();
	}

	public Expression getOperand() {
		return operand;
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		bld.append(getOperator());
		appendOperand(bld, rootVariable, operand, getPriority());
	}

	int countAccessToEverything() {
		return operand.countAccessToEverything();
	}
}