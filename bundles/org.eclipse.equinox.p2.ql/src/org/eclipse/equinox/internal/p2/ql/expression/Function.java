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

import org.eclipse.equinox.p2.ql.IEvaluationContext;

/**
 * An expression that represents a function such as filter(&lt;expr&gt;) or version(&lt;expr&gt;)
 */
abstract class Function extends NAry {

	private Object instance;

	Function(Expression[] operands) {
		super(operands);
	}

	public Object evaluate(IEvaluationContext context) {
		if (instance != null)
			return instance;

		Expression operand = operands[0];
		Object arg = operand.evaluate(context);
		if (assertSingleArgumentClass(arg)) {
			Object result = createInstance(arg);
			if (operand instanceof Constant || operand instanceof Parameter)
				// operand won't change over time so we can cache this instance.
				instance = result;
			return result;
		}
		String what = arg == null ? "null" : ("a " + arg.getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		throw new IllegalArgumentException("Cannot create a " + getOperator() + " from " + what); //$NON-NLS-1$ //$NON-NLS-2$
	}

	boolean assertSingleArgumentClass(Object v) {
		return true;
	}

	public int getExpressionType() {
		return TYPE_FUNCTION;
	}

	public void toString(StringBuffer bld) {
		bld.append(getOperator());
		bld.append('(');
		Array.elementsToString(bld, operands);
		bld.append(')');
	}

	final Object createInstance(String arg) {
		throw new UnsupportedOperationException();
	}

	Object createInstance(Object arg) {
		throw new UnsupportedOperationException();
	}

	int getPriority() {
		return PRIORITY_CONSTRUCTOR;
	}

	boolean isBoolean() {
		return false;
	}

	boolean isCollection() {
		return false;
	}
}
