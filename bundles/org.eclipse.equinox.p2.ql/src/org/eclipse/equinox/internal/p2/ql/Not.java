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
 * An expression that yields <code>true</code> when its operand does not.
 */
public final class Not extends Unary {
	public static final String OPERATOR = "!"; //$NON-NLS-1$

	public Not(Expression operand) {
		super(operand);
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		return Boolean.valueOf(operand.evaluate(context, scope) != Boolean.TRUE);
	}

	String getOperator() {
		return OPERATOR;
	}

	int getPriority() {
		return ExpressionParser.PRIORITY_NOT;
	}
}
