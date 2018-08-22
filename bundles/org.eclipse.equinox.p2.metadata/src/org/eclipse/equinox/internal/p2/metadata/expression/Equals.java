/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
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
 * An expression that performs the == and != comparisons
 */
final class Equals extends Binary {
	final boolean negate;

	Equals(Expression lhs, Expression rhs, boolean negate) {
		super(lhs, rhs);
		this.negate = negate;
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		Object lhsVal = lhs.evaluate(context);
		Object rhsVal = rhs.evaluate(context);

		// Handle collections as per the OSGi LDAP spec
		if (lhsVal instanceof Collection<?>) {
			for (Object lhsItem : (Collection<?>) lhsVal) {
				boolean eq = CoercingComparator.coerceAndEquals(lhsItem, rhsVal);

				if (eq && !negate) {
					return true;
				}
			}
			return negate;
		}

		boolean eq = CoercingComparator.coerceAndEquals(lhsVal, rhsVal);
		return negate ? !eq : eq;
	}

	@Override
	public int getExpressionType() {
		return negate ? TYPE_NOT_EQUALS : TYPE_EQUALS;
	}

	@Override
	public String getOperator() {
		return negate ? OPERATOR_NOT_EQUALS : OPERATOR_EQUALS;
	}

	@Override
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
