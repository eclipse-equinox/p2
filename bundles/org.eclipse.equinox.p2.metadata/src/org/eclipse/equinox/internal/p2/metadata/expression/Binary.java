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

import org.eclipse.equinox.internal.p2.metadata.expression.Member.DynamicMember;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IExpressionVisitor;

/**
 * The abstract base class for all binary operations
 */
public abstract class Binary extends Expression {
	public final Expression lhs;

	public final Expression rhs;

	protected Binary(Expression lhs, Expression rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public boolean accept(IExpressionVisitor visitor) {
		return super.accept(visitor) && lhs.accept(visitor) && rhs.accept(visitor);
	}

	public int compareTo(Expression e) {
		int cmp = super.compareTo(e);
		if (cmp == 0) {
			Binary be = (Binary) e;
			cmp = lhs.compareTo(be.lhs);
			if (cmp == 0)
				cmp = rhs.compareTo(be.rhs);
		}
		return cmp;
	}

	public boolean equals(Object o) {
		if (super.equals(o)) {
			Binary bo = (Binary) o;
			return lhs.equals(bo.lhs) && rhs.equals(bo.rhs);
		}
		return false;
	}

	public int getPriority() {
		return PRIORITY_BINARY; // Default priority
	}

	public int hashCode() {
		int result = 31 + lhs.hashCode();
		return 31 * result + rhs.hashCode();
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		appendOperand(bld, rootVariable, lhs, getPriority());
		bld.append(' ');
		bld.append(getOperator());
		bld.append(' ');
		appendOperand(bld, rootVariable, rhs, getPriority());
	}

	/**
	 * Appends the LDAP filter attribute name from the lhs expression if
	 * possible. 
	 * @throws UnsupportedOperationException when this expression does not conform to an
	 * LDAP filter binary expression
	 */
	void appendLDAPAttribute(StringBuffer buf) {
		if (lhs instanceof DynamicMember) {
			DynamicMember attr = (DynamicMember) lhs;
			if (attr.operand instanceof Variable) {
				buf.append(attr.getName());
				return;
			}
		}
		throw new UnsupportedOperationException();
	}

	private static char hexChar(int value) {
		return (char) (value < 10 ? ('0' + value) : ('a' + (value - 10)));
	}

	static void appendLDAPEscaped(StringBuffer bld, String str) {
		appendLDAPEscaped(bld, str, true);
	}

	static void appendLDAPEscaped(StringBuffer bld, String str, boolean escapeWild) {
		int top = str.length();
		for (int idx = 0; idx < top; ++idx) {
			char c = str.charAt(idx);
			if (!escapeWild) {
				if (c == '*') {
					bld.append(c);
					continue;
				} else if (c == '\\' && idx + 1 < top && str.charAt(idx + 1) == '*') {
					bld.append("\\2a"); //$NON-NLS-1$
					++idx;
					continue;
				}
			}
			if (c == '(' || c == ')' || c == '*' || c == '\\' || c < ' ' || c > 127) {
				short cs = (short) c;
				bld.append('\\');
				bld.append(hexChar((cs & 0x00f0) >> 4));
				bld.append(hexChar(cs & 0x000f));
			} else
				bld.append(c);
		}
	}

	/**
	 * Appends the LDAP filter value from the rhs expression if
	 * possible. 
	 * @throws UnsupportedOperationException when this expression does not conform to an
	 * LDAP filter binary expression
	 */
	void appendLDAPValue(StringBuffer buf) {
		if (rhs instanceof Literal) {
			Object value = rhs.evaluate(null);
			if (value instanceof String || value instanceof Version) {
				appendLDAPEscaped(buf, value.toString());
				return;
			}
		}
		throw new UnsupportedOperationException();
	}

	int countAccessToEverything() {
		return lhs.countAccessToEverything() + rhs.countAccessToEverything();
	}
}
