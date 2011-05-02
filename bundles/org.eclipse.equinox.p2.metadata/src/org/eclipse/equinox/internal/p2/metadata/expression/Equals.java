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
 * An expression that performs the == and != comparisons
 */
final class Equals extends Binary {
	final boolean negate;

	Equals(Expression lhs, Expression rhs, boolean negate) {
		super(lhs, rhs);
		this.negate = negate;
	}

	public Object evaluate(IEvaluationContext context) {
		boolean result = CoercingComparator.coerceAndEquals(lhs.evaluate(context), rhs.evaluate(context));
		if (negate)
			result = !result;
		return Boolean.valueOf(result);
	}

	public int getExpressionType() {
		return negate ? TYPE_NOT_EQUALS : TYPE_EQUALS;
	}

	public String getOperator() {
		return negate ? OPERATOR_NOT_EQUALS : OPERATOR_EQUALS;
	}

	public void toLDAPString(StringBuffer buf) {
		if (negate)
			buf.append("(!"); //$NON-NLS-1$
		buf.append('(');
		appendLDAPAttribute(buf);
		buf.append('=');
		appendLDAPValue(buf);
		buf.append(')');
		if (negate)
			buf.append(')');
	}
}
