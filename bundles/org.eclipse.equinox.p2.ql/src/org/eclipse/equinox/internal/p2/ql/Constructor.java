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
 * An expression that represents a constructor such as filter(&lt;expr&gt;) or version(&lt;expr&gt;)
 */
public abstract class Constructor extends NAry {

	private Object instance;

	Constructor(Expression[] operands) {
		super(operands);
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		if (instance != null)
			return instance;

		Expression operand = operands[0];
		Object arg = operand.evaluate(context, scope);
		if (arg instanceof String) {
			Object result = createInstance((String) arg);
			if (operand instanceof Constant || operand instanceof Parameter)
				// operand won't change over time so we can cache this instance.
				instance = result;
			return result;
		}
		String what = arg == null ? "null" : ("a " + arg.getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		throw new IllegalArgumentException("Cannot create a " + getOperator() + " from " + what); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void toString(StringBuffer bld) {
		bld.append(getOperator());
		bld.append('(');
		Array.elementsToString(bld, operands);
		bld.append(')');
	}

	Object createInstance(String arg) {
		throw new UnsupportedOperationException();
	}

	int getPriority() {
		return ExpressionParser.PRIORITY_CONSTRUCTOR;
	}
}
