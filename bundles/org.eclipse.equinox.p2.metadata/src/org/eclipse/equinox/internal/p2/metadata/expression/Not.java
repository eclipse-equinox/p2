/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
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
 * An expression that yields <code>true</code> when its operand does not.
 */
final class Not extends Unary {
	Not(Expression operand) {
		super(operand);
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		return Boolean.valueOf(operand.evaluate(context) != Boolean.TRUE);
	}

	@Override
	public int getExpressionType() {
		return TYPE_NOT;
	}

	@Override
	public String getOperator() {
		return OPERATOR_NOT;
	}

	@Override
	public int getPriority() {
		return PRIORITY_NOT;
	}

	@Override
	public int hashCode() {
		return 3 * operand.hashCode();
	}

	@Override
	public void toLDAPString(StringBuffer buf) {
		buf.append("(!"); //$NON-NLS-1$
		operand.toLDAPString(buf);
		buf.append(')');
	}
}
