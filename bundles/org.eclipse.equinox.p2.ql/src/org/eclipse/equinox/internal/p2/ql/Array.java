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
 * An array of expressions
 */
public final class Array extends NAry {
	static final String OPERATOR = "[]"; //$NON-NLS-1$

	static void elementsToString(StringBuffer bld, Expression[] elements) {
		int top = elements.length;
		if (top > 0) {
			elements[0].toString(bld);
			for (int idx = 1; idx < top; ++idx) {
				bld.append(", "); //$NON-NLS-1$
				appendOperand(bld, elements[idx], ExpressionParser.PRIORITY_COMMA);
			}
		}
	}

	public Array(Expression[] operands) {
		super(assertLength(operands, 0, OPERATOR));
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		return new ArrayIterator(context, scope, operands);
	}

	public void toString(StringBuffer bld) {
		bld.append('[');
		elementsToString(bld, operands);
		bld.append(']');
	}

	String getOperator() {
		return OPERATOR;
	}

	int getPriority() {
		return ExpressionParser.PRIORITY_CONSTRUCTOR;
	}
}
