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

import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 * n-ary AND operator. The full evaluation is <code>true</code> if all its operands evaluate to
 * <code>true</code>. 
 */
final class And extends NAry {
	And(Expression[] operands) {
		super(assertLength(operands, 2, OPERATOR_AND));
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		for (int idx = 0; idx < operands.length; ++idx) {
			if (operands[idx].evaluate(context) != Boolean.TRUE)
				return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	@Override
	public int getExpressionType() {
		return TYPE_AND;
	}

	@Override
	public String getOperator() {
		return OPERATOR_AND;
	}

	@Override
	public int getPriority() {
		return PRIORITY_AND;
	}

	@Override
	public void toLDAPString(StringBuffer buf) {
		buf.append("(&"); //$NON-NLS-1$
		for (int idx = 0; idx < operands.length; ++idx)
			operands[idx].toLDAPString(buf);
		buf.append(')');
	}
}
