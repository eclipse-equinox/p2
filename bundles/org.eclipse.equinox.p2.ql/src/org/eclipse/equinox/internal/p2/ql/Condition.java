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
 * Comparisons for magnitude.
 */
public final class Condition extends Binary {
	static final String IF_OPERATOR = "?"; //$NON-NLS-1$
	static final String ELSE_OPERATOR = ":"; //$NON-NLS-1$

	final Expression ifFalse;

	public Condition(Expression test, Expression ifTrue, Expression ifFalse) {
		super(test, ifTrue);
		this.ifFalse = ifFalse;
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		return lhs.evaluate(context, scope) == Boolean.TRUE ? rhs.evaluate(context, scope) : ifFalse.evaluate(context, scope);
	}

	public void toString(StringBuffer bld) {
		super.toString(bld);
		bld.append(' ');
		bld.append(ELSE_OPERATOR);
		bld.append(' ');
		appendOperand(bld, ifFalse, getPriority());
	}

	String getOperator() {
		return IF_OPERATOR;
	}

	int getPriority() {
		return ExpressionParser.PRIORITY_CONDITION;
	}
}
