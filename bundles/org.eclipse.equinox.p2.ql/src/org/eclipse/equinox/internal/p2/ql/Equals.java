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
 * An expression that performs the == and != comparisons
 */
public final class Equals extends Binary {
	static final String EQUALS_OPERATOR = "=="; //$NON-NLS-1$
	static final String NOT_EQUALS_OPERATOR = "!="; //$NON-NLS-1$

	private final boolean negate;

	public Equals(Expression lhs, Expression rhs) {
		this(lhs, rhs, false);
	}

	public Equals(Expression lhs, Expression rhs, boolean negate) {
		super(lhs, rhs);
		this.negate = negate;
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		Object lval = lhs.evaluate(context, scope);
		Object rval = rhs.evaluate(context, scope);
		boolean result;
		if (lval == null || rval == null)
			result = lval == rval;
		else {
			if (lval.getClass() != rval.getClass()) {
				if (lval instanceof String)
					rval = rval.toString();
				else if (rval instanceof String)
					lval = lval.toString();
			}
			result = lval.equals(rval);
		}
		if (negate)
			result = !result;
		return Boolean.valueOf(result);
	}

	String getOperator() {
		return negate ? NOT_EQUALS_OPERATOR : EQUALS_OPERATOR;
	}
}
