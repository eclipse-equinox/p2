/*******************************************************************************
 * Copyright (c) 2010, 2017 Cloudsmith Inc. and others.
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

import org.eclipse.equinox.p2.metadata.expression.IExpressionVisitor;

/**
 * The abstract baseclass for all N-ary expressions
 */
public abstract class NAry extends Expression {
	public final Expression[] operands;

	protected NAry(Expression[] operands) {
		this.operands = operands;
	}

	@Override
	public boolean accept(IExpressionVisitor visitor) {
		if (super.accept(visitor))
			for (Expression operand : operands)
				if (!operand.accept(visitor))
					return false;
		return true;
	}

	@Override
	public int compareTo(Expression e) {
		int cmp = super.compareTo(e);
		if (cmp == 0)
			cmp = compare(operands, ((NAry) e).operands);
		return cmp;
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o) && equals(operands, ((NAry) o).operands);
	}

	@Override
	public abstract String getOperator();

	@Override
	public int hashCode() {
		return hashCode(operands);
	}

	@Override
	public void toString(StringBuilder bld, Variable rootVariable) {
		appendOperand(bld, rootVariable, operands[0], getPriority());
		for (int idx = 1; idx < operands.length; ++idx) {
			bld.append(' ');
			bld.append(getOperator());
			bld.append(' ');
			appendOperand(bld, rootVariable, operands[idx], getPriority());
		}
	}

	@Override
	int countAccessToEverything() {
		int cnt = 0;
		for (Expression operand : operands)
			cnt += operand.countAccessToEverything();
		return cnt;
	}
}
