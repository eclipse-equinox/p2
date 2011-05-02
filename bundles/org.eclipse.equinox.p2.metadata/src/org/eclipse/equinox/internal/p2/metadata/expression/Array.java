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
import java.util.NoSuchElementException;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

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

	Array(Expression[] operands) {
		super(assertLength(operands, 0, OPERATOR_ARRAY));
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

	public void toString(StringBuffer bld, Variable rootVariable) {
		bld.append('[');
		elementsToString(bld, rootVariable, operands);
		bld.append(']');
	}

	public String getOperator() {
		return OPERATOR_ARRAY;
	}

	public int getPriority() {
		return PRIORITY_FUNCTION;
	}
}
