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

import java.util.*;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
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

	@Override
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

	@Override
	public boolean equals(Object o) {
		if (super.equals(o)) {
			Literal bo = (Literal) o;
			return value == null ? bo.value == null : value.equals(bo.value);
		}
		return false;
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		return value;
	}

	@Override
	public int getExpressionType() {
		return TYPE_LITERAL;
	}

	@Override
	public String getOperator() {
		return "<literal>"; //$NON-NLS-1$
	}

	@Override
	public int getPriority() {
		return PRIORITY_LITERAL;
	}

	@Override
	public int hashCode() {
		return 31 + value.hashCode();
	}

	@Override
	public void toLDAPString(StringBuilder buf) {
		if (!(value instanceof Filter))
			throw new UnsupportedOperationException();
		buf.append(value.toString());
	}

	@Override
	public void toString(StringBuilder bld, Variable rootVariable) {
		appendValue(bld, value);
	}

	private static void appendValue(StringBuilder bld, Object value) {
		if (value == null)
			bld.append("null"); //$NON-NLS-1$
		else if (value == Boolean.TRUE)
			bld.append("true"); //$NON-NLS-1$
		else if (value == Boolean.FALSE)
			bld.append("false"); //$NON-NLS-1$
		else if (value instanceof String)
			appendQuotedString(bld, (String) value);
		else if (value instanceof Number)
			bld.append(value.toString());
		else if (value instanceof SimplePattern) {
			appendEscaped(bld, '/', value.toString());
		} else if (value instanceof Version) {
			bld.append("version("); //$NON-NLS-1$
			appendQuotedString(bld, value.toString());
			bld.append(')');
		} else if (value instanceof VersionRange) {
			bld.append("range("); //$NON-NLS-1$
			appendQuotedString(bld, value.toString());
			bld.append(')');
		} else if (value instanceof Class<?>) {
			bld.append("class("); //$NON-NLS-1$
			appendQuotedString(bld, value.toString());
			bld.append(')');
		} else if (value instanceof Filter) {
			bld.append("filter("); //$NON-NLS-1$
			appendQuotedString(bld, value.toString());
			bld.append(')');
		} else if (value instanceof Set<?>) {
			bld.append("set("); //$NON-NLS-1$
			appendLiteralCollection(bld, (Collection<?>) value);
			bld.append(')');
		} else if (value instanceof Collection<?>)
			appendLiteralCollection(bld, (Collection<?>) value);
		else
			bld.append(value);

	}

	private static void appendLiteralCollection(StringBuilder bld, Collection<?> collection) {
		bld.append('[');
		Iterator<?> iter = collection.iterator();
		if (iter.hasNext()) {
			appendValue(bld, iter.next());
			while (iter.hasNext()) {
				bld.append(',');
				appendValue(bld, iter.next());
			}
		}
		bld.append(']');
	}

	private static void appendQuotedString(StringBuilder bld, String str) {
		if (str.indexOf('\'') < 0) {
			bld.append('\'');
			bld.append(str);
			bld.append('\'');
		} else
			appendEscaped(bld, '"', str);
	}

	private static void appendEscaped(StringBuilder bld, char delimiter, String str) {
		bld.append(delimiter);
		int top = str.length();
		for (int idx = 0; idx < top; ++idx) {
			char c = str.charAt(idx);
			if (c == delimiter || c == '\\')
				bld.append('\\');
			bld.append(c);
		}
		bld.append(delimiter);
	}
}
