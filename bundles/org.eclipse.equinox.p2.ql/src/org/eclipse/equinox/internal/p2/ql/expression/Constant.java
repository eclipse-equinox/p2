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

import org.eclipse.equinox.p2.query.IQueryResult;

import java.util.*;
import org.eclipse.equinox.p2.ql.IEvaluationContext;
import org.eclipse.equinox.p2.ql.SimplePattern;

/**
 * An expression that represents a constant value.
 */
final class Constant extends Expression {
	private final Object value;

	static final Constant NULL_CONSTANT = new Constant(null);

	static final Constant TRUE_CONSTANT = new Constant(Boolean.TRUE);

	static final Constant FALSE_CONSTANT = new Constant(Boolean.FALSE);

	static Constant create(Object value) {
		if (value == null)
			return NULL_CONSTANT;
		if (value == Boolean.TRUE)
			return TRUE_CONSTANT;
		if (value == Boolean.FALSE)
			return FALSE_CONSTANT;
		return new Constant(value);
	}

	private Constant(Object value) {
		this.value = value;
	}

	public Object evaluate(IEvaluationContext context) {
		return value;
	}

	public int getExpressionType() {
		return TYPE_LITERAL;
	}

	public void toString(StringBuffer bld) {
		if (value == null)
			bld.append("null"); //$NON-NLS-1$
		else if (value instanceof String) {
			String str = (String) value;
			char sep = str.indexOf('\'') >= 0 ? '"' : '\'';
			bld.append(sep);
			bld.append(str);
			bld.append(sep);
		} else if (value instanceof SimplePattern) {
			appendEscaped(bld, '/', value.toString());
		} else
			bld.append(value);
	}

	int getPriority() {
		return PRIORITY_LITERAL;
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

	boolean isBoolean() {
		return value instanceof Boolean;
	}

	boolean isCollection() {
		return value instanceof Collection<?> || value instanceof Map<?, ?> || value instanceof Iterator<?> || value instanceof IQueryResult<?> || value != null && value.getClass().isArray();
	}

	String getOperator() {
		return "<literal>"; //$NON-NLS-1$
	}
}
