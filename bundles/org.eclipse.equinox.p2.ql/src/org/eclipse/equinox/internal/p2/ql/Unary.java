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
package org.eclipse.equinox.internal.p2.ql;

/**
 * The abstract base class for all unary expressions
 */
abstract class Unary extends Expression {
	final Expression operand;

	Unary(Expression operand) {
		this.operand = operand;
	}

	public boolean accept(Visitor visitor) {
		return super.accept(visitor) && operand.accept(visitor);
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		return operand.evaluate(context, scope);
	}

	public void toString(StringBuffer bld) {
		bld.append(getOperator());
		appendOperand(bld, operand, getPriority());
	}

	int countReferenceToEverything() {
		return operand.countReferenceToEverything();
	}

	abstract String getOperator();
}