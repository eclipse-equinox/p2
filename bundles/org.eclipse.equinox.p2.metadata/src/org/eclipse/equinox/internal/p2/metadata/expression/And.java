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

import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 * n-ary AND operator. The full evaluation is <code>true</code> if all its operands evaluate to
 * <code>true</code>. 
 */
final class And extends NAry {
	And(Expression[] operands) {
		super(assertLength(operands, 2, OPERATOR_AND));
	}

	public Object evaluate(IEvaluationContext context) {
		for (int idx = 0; idx < operands.length; ++idx) {
			if (operands[idx].evaluate(context) != Boolean.TRUE)
				return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	public int getExpressionType() {
		return TYPE_AND;
	}

	public String getOperator() {
		return OPERATOR_AND;
	}

	public int getPriority() {
		return PRIORITY_AND;
	}

	public void toLDAPString(StringBuffer buf) {
		buf.append("(&"); //$NON-NLS-1$
		for (int idx = 0; idx < operands.length; ++idx)
			operands[idx].toLDAPString(buf);
		buf.append(')');
	}
}
