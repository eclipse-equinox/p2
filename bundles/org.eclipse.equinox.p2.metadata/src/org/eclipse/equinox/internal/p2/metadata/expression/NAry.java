/*******************************************************************************
 * Copyright (c) 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	public boolean accept(IExpressionVisitor visitor) {
		if (super.accept(visitor))
			for (int idx = 0; idx < operands.length; ++idx)
				if (!operands[idx].accept(visitor))
					return false;
		return true;
	}

	public int compareTo(Expression e) {
		int cmp = super.compareTo(e);
		if (cmp == 0)
			cmp = compare(operands, ((NAry) e).operands);
		return cmp;
	}

	public boolean equals(Object o) {
		return super.equals(o) && equals(operands, ((NAry) o).operands);
	}

	public abstract String getOperator();

	public int hashCode() {
		return hashCode(operands);
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		appendOperand(bld, rootVariable, operands[0], getPriority());
		for (int idx = 1; idx < operands.length; ++idx) {
			bld.append(' ');
			bld.append(getOperator());
			bld.append(' ');
			appendOperand(bld, rootVariable, operands[idx], getPriority());
		}
	}

	int countAccessToEverything() {
		int cnt = 0;
		for (int idx = 0; idx < operands.length; ++idx)
			cnt += operands[idx].countAccessToEverything();
		return cnt;
	}
}
