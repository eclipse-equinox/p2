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
 * An expression that performs the == and != comparisons
 */
final class Equals extends Binary {
	final boolean negate;

	Equals(Expression lhs, Expression rhs, boolean negate) {
		super(lhs, rhs);
		this.negate = negate;
		assertNotCollection(lhs, "lhs"); //$NON-NLS-1$
		assertNotCollection(rhs, "rhs"); //$NON-NLS-1$
	}

	public Object evaluate(IEvaluationContext context) {
		Object lval = lhs.evaluate(context);
		Object rval = rhs.evaluate(context);
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

	public int getExpressionType() {
		return negate ? TYPE_NOT_EQUALS : TYPE_EQUALS;
	}

	String getOperator() {
		return negate ? OPERATOR_NOT_EQUALS : OPERATOR_EQUALS;
	}

	boolean isBoolean() {
		return true;
	}

}
