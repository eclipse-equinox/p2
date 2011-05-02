/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
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
 * An expression that yields <code>true</code> when its operand does not.
 */
final class Not extends Unary {
	Not(Expression operand) {
		super(operand);
	}

	public Object evaluate(IEvaluationContext context) {
		return Boolean.valueOf(operand.evaluate(context) != Boolean.TRUE);
	}

	public int getExpressionType() {
		return TYPE_NOT;
	}

	public String getOperator() {
		return OPERATOR_NOT;
	}

	public int getPriority() {
		return PRIORITY_NOT;
	}

	public int hashCode() {
		return 3 * operand.hashCode();
	}

	public void toLDAPString(StringBuffer buf) {
		buf.append("(!"); //$NON-NLS-1$
		operand.toLDAPString(buf);
		buf.append(')');
	}
}
