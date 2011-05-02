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

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 * An expression that represents a function such as filter(&lt;expr&gt;) or version(&lt;expr&gt;)
 */
public abstract class Function extends NAry {

	private Object instance;

	protected Function(Expression[] operands) {
		super(operands);
	}

	boolean assertSingleArgumentClass(Object v) {
		return true;
	}

	Object createInstance(Object arg) {
		throw new UnsupportedOperationException();
	}

	final Object createInstance(String arg) {
		throw new UnsupportedOperationException();
	}

	public Object evaluate(IEvaluationContext context) {
		if (instance != null)
			return instance;

		Expression operand = operands[0];
		Object arg = operand.evaluate(context);
		if (assertSingleArgumentClass(arg)) {
			Object result = createInstance(arg);
			if (operand instanceof Literal || operand instanceof Parameter)
				// operand won't change over time so we can cache this instance.
				instance = result;
			return result;
		}
		String what = arg == null ? "null" : ("a " + arg.getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		throw new IllegalArgumentException("Cannot create a " + getOperator() + " from " + what); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		Object value = evaluate(context);
		if (!(value instanceof Iterator<?>))
			value = RepeatableIterator.create(value);
		return (Iterator<?>) value;
	}

	public int getExpressionType() {
		return TYPE_FUNCTION;
	}

	public int getPriority() {
		return PRIORITY_FUNCTION;
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		bld.append(getOperator());
		bld.append('(');
		elementsToString(bld, rootVariable, operands);
		bld.append(')');
	}
}
