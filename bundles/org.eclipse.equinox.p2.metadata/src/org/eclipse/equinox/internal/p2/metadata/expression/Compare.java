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

import java.util.Collection;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 * Comparisons for magnitude.
 */
final class Compare extends Binary {
	static IllegalArgumentException uncomparable(Object lval, Object rval) {
		return new IllegalArgumentException("Cannot compare a " + lval.getClass().getName() + " to a " + rval.getClass().getName()); //$NON-NLS-1$//$NON-NLS-2$
	}

	final boolean compareLess;

	final boolean equalOK;

	Compare(Expression lhs, Expression rhs, boolean compareLess, boolean equalOK) {
		super(lhs, rhs);
		this.compareLess = compareLess;
		this.equalOK = equalOK;
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		Object lhsVal = lhs.evaluate(context);
		Object rhsVal = rhs.evaluate(context);

		// Handle collections as per the OSGi LDAP spec
		if (lhsVal instanceof Collection<?>) {
			for (Object lhsItem : (Collection<?>) lhsVal) {
				int cmpResult = CoercingComparator.coerceAndCompare(lhsItem, rhsVal);

				if (cmpResult == 0) {
					return equalOK;
				}

				if (cmpResult < 0 && compareLess) {
					return true;
				}

				if (!compareLess) {
					return true;
				}
			}
			return false;
		}

		int cmpResult = CoercingComparator.coerceAndCompare(lhsVal, rhsVal);
		return Boolean.valueOf(cmpResult == 0 ? equalOK : (cmpResult < 0 ? compareLess : !compareLess));
	}

	@Override
	public int getExpressionType() {
		return compareLess ? (equalOK ? TYPE_LESS_EQUAL : TYPE_LESS) : (equalOK ? TYPE_GREATER_EQUAL : TYPE_GREATER);
	}

	@Override
	public String getOperator() {
		return compareLess ? (equalOK ? OPERATOR_LT_EQUAL : OPERATOR_LT) : (equalOK ? OPERATOR_GT_EQUAL : OPERATOR_GT);
	}

	@Override
	public void toLDAPString(StringBuffer buf) {
		if (!equalOK)
			buf.append("(!"); //$NON-NLS-1$
		buf.append('(');
		appendLDAPAttribute(buf);
		if (equalOK)
			buf.append(compareLess ? OPERATOR_LT_EQUAL : OPERATOR_GT_EQUAL);
		else
			buf.append(compareLess ? OPERATOR_GT_EQUAL : OPERATOR_LT_EQUAL);
		appendLDAPValue(buf);
		buf.append(')');
		if (!equalOK)
			buf.append(')');
	}
}
