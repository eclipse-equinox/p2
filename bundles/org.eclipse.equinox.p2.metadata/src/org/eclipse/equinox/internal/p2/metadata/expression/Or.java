/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
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

/**
 * n-ary OR operator. The full evaluation is <code>false</code> if none of its operands
 * evaluate to <code>true</code>.
 */
final class Or extends NAry {
	public Or(Expression[] operands) {
		super(assertLength(operands, 2, OPERATOR_OR));
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		for (int idx = 0; idx < operands.length; ++idx) {
			if (operands[idx].evaluate(context) == Boolean.TRUE)
				return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	@Override
	public int getExpressionType() {
		return TYPE_OR;
	}

	@Override
	public String getOperator() {
		return OPERATOR_OR;
	}

	@Override
	public int getPriority() {
		return PRIORITY_OR;
	}

	@Override
	public void toLDAPString(StringBuffer buf) {
		buf.append("(|"); //$NON-NLS-1$
		for (int idx = 0; idx < operands.length; ++idx)
			operands[idx].toLDAPString(buf);
		buf.append(')');
	}
}
