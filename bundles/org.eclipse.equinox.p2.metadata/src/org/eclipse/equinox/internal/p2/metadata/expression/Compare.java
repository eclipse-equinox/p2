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

	public Object evaluate(IEvaluationContext context) {
		int cmpResult = CoercingComparator.coerceAndCompare(lhs.evaluate(context), rhs.evaluate(context));
		return Boolean.valueOf(cmpResult == 0 ? equalOK : (cmpResult < 0 ? compareLess : !compareLess));
	}

	public int getExpressionType() {
		return compareLess ? (equalOK ? TYPE_LESS_EQUAL : TYPE_LESS) : (equalOK ? TYPE_GREATER_EQUAL : TYPE_GREATER);
	}

	public String getOperator() {
		return compareLess ? (equalOK ? OPERATOR_LT_EQUAL : OPERATOR_LT) : (equalOK ? OPERATOR_GT_EQUAL : OPERATOR_GT);
	}

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
