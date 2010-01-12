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

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.eclipse.equinox.internal.p2.ql.parser.IParserConstants;
import org.eclipse.equinox.p2.ql.IEvaluationContext;

/**
 * An array of expressions
 */
final class Array extends NAry {
	final class ArrayIterator implements Iterator<Object> {
		private final IEvaluationContext context;

		private int pos = -1;

		public ArrayIterator(IEvaluationContext context) {
			this.context = context;
		}

		public boolean hasNext() {
			return pos + 1 < operands.length;
		}

		public Object next() {
			if (++pos >= operands.length) {
				--pos;
				throw new NoSuchElementException();
			}
			return operands[pos].evaluate(context);
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	static void elementsToString(StringBuffer bld, Expression[] elements) {
		int top = elements.length;
		if (top > 0) {
			elements[0].toString(bld);
			for (int idx = 1; idx < top; ++idx) {
				bld.append(", "); //$NON-NLS-1$
				appendOperand(bld, elements[idx], PRIORITY_COMMA);
			}
		}
	}

	Array(Expression[] operands) {
		super(assertLength(operands, 0, IParserConstants.OPERATOR_ARRAY));
	}

	public Object evaluate(IEvaluationContext context) {
		return evaluateAsIterator(context);
	}

	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		return new ArrayIterator(context);
	}

	public int getExpressionType() {
		return TYPE_ARRAY;
	}

	public void toString(StringBuffer bld) {
		bld.append('[');
		elementsToString(bld, operands);
		bld.append(']');
	}

	String getOperator() {
		return IParserConstants.OPERATOR_ARRAY;
	}

	int getPriority() {
		return PRIORITY_CONSTRUCTOR;
	}

	boolean isBoolean() {
		return false;
	}

	boolean isCollection() {
		return true;
	}

	boolean isElementBoolean() {
		return super.isBoolean();
	}
}
