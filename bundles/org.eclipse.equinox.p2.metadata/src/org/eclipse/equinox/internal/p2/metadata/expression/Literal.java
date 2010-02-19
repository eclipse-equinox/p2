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
package org.eclipse.equinox.internal.p2.metadata.expression;

import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.SimplePattern;
import org.osgi.framework.Filter;

/**
 * An expression that represents a constant value.
 */
public final class Literal extends Expression {
	public static final Literal FALSE_CONSTANT = new Literal(Boolean.FALSE);

	public static final Literal NULL_CONSTANT = new Literal(null);

	public static final Literal TRUE_CONSTANT = new Literal(Boolean.TRUE);

	public static Literal create(Object value) {
		if (value == null)
			return NULL_CONSTANT;
		if (value == Boolean.TRUE)
			return TRUE_CONSTANT;
		if (value == Boolean.FALSE)
			return FALSE_CONSTANT;
		return new Literal(value);
	}

	public final Object value;

	private Literal(Object value) {
		this.value = value;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public int compareTo(Expression e) {
		int cmp = super.compareTo(e);
		if (cmp != 0)
			return cmp;

		Object eValue = ((Literal) e).value;
		if (value == null)
			return eValue == null ? 0 : -1;

		if (eValue == null)
			return 1;

		if (eValue.getClass() == value.getClass())
			return ((Comparable) value).compareTo(eValue);

		return eValue.getClass().getName().compareTo(value.getClass().getName());
	}

	public boolean equals(Object o) {
		if (super.equals(o)) {
			Literal bo = (Literal) o;
			return value == null ? bo.value == null : value.equals(bo.value);
		}
		return false;
	}

	public Object evaluate(IEvaluationContext context) {
		return value;
	}

	public int getExpressionType() {
		return TYPE_LITERAL;
	}

	public String getOperator() {
		return "<literal>"; //$NON-NLS-1$
	}

	public int getPriority() {
		return PRIORITY_LITERAL;
	}

	public int hashCode() {
		return 31 + value.hashCode();
	}

	public void toLDAPString(StringBuffer buf) {
		if (!(value instanceof Filter))
			throw new UnsupportedOperationException();
		buf.append(value.toString());
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		if (value == null)
			bld.append("null"); //$NON-NLS-1$
		else if (value instanceof String || value instanceof Version) {
			String str = value.toString();
			char sep = str.indexOf('\'') >= 0 ? '"' : '\'';
			bld.append(sep);
			bld.append(str);
			bld.append(sep);
		} else if (value instanceof SimplePattern) {
			appendEscaped(bld, '/', value.toString());
		} else
			bld.append(value);
	}

	private void appendEscaped(StringBuffer bld, char delimiter, String str) {
		bld.append(delimiter);
		int top = str.length();
		for (int idx = 0; idx < top; ++idx) {
			char c = str.charAt(idx);
			if (c == delimiter)
				bld.append('\\');
			bld.append(c);
		}
		bld.append(delimiter);
	}
}
