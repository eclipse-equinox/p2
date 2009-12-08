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

import org.eclipse.equinox.internal.provisional.p2.metadata.Version;

/**
 * Comparisons for magnitude.
 */
public final class Compare extends Binary {
	static final String LT_OPERATOR = "<"; //$NON-NLS-1$
	static final String LT_EQUAL_OPERATOR = "<="; //$NON-NLS-1$
	static final String GT_OPERATOR = ">"; //$NON-NLS-1$
	static final String GT_EQUAL_OPERATOR = ">="; //$NON-NLS-1$

	private final boolean compareLess;
	private final boolean equalOK;

	public Compare(Expression lhs, Expression rhs, boolean compareLess, boolean equalOK) {
		super(lhs, rhs);
		this.compareLess = compareLess;
		this.equalOK = equalOK;
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		Object lval = lhs.evaluate(context, scope);
		Object rval = rhs.evaluate(context, scope);
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

	String getOperator() {
		return compareLess ? (equalOK ? LT_EQUAL_OPERATOR : LT_OPERATOR) : (equalOK ? GT_EQUAL_OPERATOR : GT_OPERATOR);
	}
}
