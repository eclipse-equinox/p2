/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ql.expression;

import org.eclipse.equinox.p2.ql.IEvaluationContext;
import org.eclipse.equinox.p2.ql.IExpressionVisitor;

/**
 * The abstract base class for all unary expressions
 */
abstract class Unary extends Expression {
	public final Expression operand;

	Unary(Expression operand) {
		this.operand = operand;
	}

	public boolean accept(IExpressionVisitor visitor) {
		return super.accept(visitor) && operand.accept(visitor);
	}

	public Object evaluate(IEvaluationContext context) {
		return operand.evaluate(context);
	}

	public void toString(StringBuffer bld) {
		bld.append(getOperator());
		appendOperand(bld, operand, getPriority());
	}

	public Expression getOperand() {
		return operand;
	}

	int countReferenceToEverything() {
		return operand.countReferenceToEverything();
	}

	abstract String getOperator();
}