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

import org.eclipse.equinox.p2.ql.IExpressionVisitor;

/**
 * The abstract baseclass for all N-ary expressions
 *
 */
abstract class NAry extends Expression {
	static Expression[] assertLength(Expression[] operands, int length, String operand) {
		if (operands == null)
			operands = emptyArray;
		if (operands.length < length)
			throw new IllegalArgumentException("Not enough operands for " + operand); //$NON-NLS-1$
		return operands;
	}

	static Expression[] assertLength(Expression[] operands, int minLength, int maxLength, String operand) {
		if (operands == null)
			operands = emptyArray;
		if (operands.length < minLength)
			throw new IllegalArgumentException("Not enough operands for " + operand); //$NON-NLS-1$
		if (operands.length > maxLength)
			throw new IllegalArgumentException("Too many operands for " + operand); //$NON-NLS-1$
		return operands;
	}

	final Expression[] operands;

	NAry(Expression[] operands) {
		this.operands = operands;
	}

	public boolean accept(IExpressionVisitor visitor) {
		if (super.accept(visitor))
			for (int idx = 0; idx < operands.length; ++idx)
				if (!operands[idx].accept(visitor))
					return false;
		return true;
	}

	public void toString(StringBuffer bld) {
		appendOperand(bld, operands[0], getPriority());
		for (int idx = 1; idx < operands.length; ++idx) {
			bld.append(' ');
			bld.append(getOperator());
			bld.append(' ');
			appendOperand(bld, operands[idx], getPriority());
		}
	}

	int countReferenceToEverything() {
		int count = 0;
		for (int idx = 0; count < 2 && idx < operands.length; ++idx)
			count += operands[idx].countReferenceToEverything();
		return count;
	}

	abstract String getOperator();

	boolean isBoolean() {
		int idx = operands.length;
		while (--idx >= 0)
			if (!operands[idx].isBoolean())
				return false;
		return true;
	}

	boolean isCollection() {
		int idx = operands.length;
		while (--idx >= 0)
			if (!operands[idx].isCollection())
				return false;
		return true;
	}

}
