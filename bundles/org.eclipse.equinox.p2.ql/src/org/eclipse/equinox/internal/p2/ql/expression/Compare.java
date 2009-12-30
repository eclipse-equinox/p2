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

import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.p2.ql.IEvaluationContext;

/**
 * Comparisons for magnitude.
 */
final class Compare extends Binary {
	final boolean compareLess;
	final boolean equalOK;

	Compare(Expression lhs, Expression rhs, boolean compareLess, boolean equalOK) {
		super(lhs, rhs);
		this.compareLess = compareLess;
		this.equalOK = equalOK;
		assertNotCollection(lhs, "lhs"); //$NON-NLS-1$
		assertNotCollection(rhs, "rhs"); //$NON-NLS-1$
	}

	public Object evaluate(IEvaluationContext context) {
		Object lval = lhs.evaluate(context);
		Object rval = rhs.evaluate(context);
		if (lval == null || rval == null)
			throw new IllegalArgumentException("Cannot compare null to anything"); //$NON-NLS-1$

		try {

			if (lval.getClass() != rval.getClass()) {
				if (lval instanceof Version && rval instanceof String)
					rval = Version.create((String) rval);
				else if (rval instanceof Version && lval instanceof String)
					lval = Version.create((String) lval);
				else if (lval instanceof String)
					rval = rval.toString();
				else if (rval instanceof String)
					lval = lval.toString();
			}

			if (lval instanceof Comparable) {
				int cmpResult = ((Comparable) lval).compareTo(rval);
				return Boolean.valueOf(cmpResult == 0 ? equalOK : (cmpResult < 0 ? compareLess : !compareLess));
			}
		} catch (Exception e) {
			//
		}
		throw new IllegalArgumentException("Cannot compare a " + lval.getClass().getName() + " to a " + rval.getClass().getName()); //$NON-NLS-1$//$NON-NLS-2$
	}

	public int getExpressionType() {
		return compareLess ? (equalOK ? TYPE_LESS_EQUAL : TYPE_LESS) : (equalOK ? TYPE_GREATER_EQUAL : TYPE_GREATER);
	}

	String getOperator() {
		return compareLess ? (equalOK ? OPERATOR_LT_EQUAL : OPERATOR_LT) : (equalOK ? OPERATOR_GT_EQUAL : OPERATOR_GT);
	}

	boolean isBoolean() {
		return true;
	}
}
