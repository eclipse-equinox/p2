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

/**
 * An expression that represents a constant value.
 */
public final class Constant extends Expression {
	private final Object value;

	public static final Constant NULL_CONSTANT = new Constant(null);

	public static final Constant TRUE_CONSTANT = new Constant(Boolean.TRUE);

	public static final Constant FALSE_CONSTANT = new Constant(Boolean.FALSE);

	static final String NULL_KEYWORD = "null"; //$NON-NLS-1$

	static final String FALSE_KEYWORD = "false"; //$NON-NLS-1$

	static final String TRUE_KEYWORD = "true"; //$NON-NLS-1$

	public Constant(Object value) {
		this.value = value;
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		return value;
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
		return ExpressionParser.PRIORITY_LITERAL;
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
