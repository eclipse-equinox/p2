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

import org.eclipse.equinox.p2.ql.IEvaluationContext;

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

	String getOperator() {
		return OPERATOR_NOT;
	}

	int getPriority() {
		return PRIORITY_NOT;
	}

	boolean isBoolean() {
		return true;
	}
}
