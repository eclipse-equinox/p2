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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A collection filter that limits the number of entries in the collection
 */
public final class Limit extends Binary {

	/**
	 * An iterator that stops iterating after a given number of iterations.
	 */
	public static final class CountingIterator implements Iterator {
		private final Iterator innerIterator;
		private int counter;

		public CountingIterator(Iterator iterator, int count) {
			this.innerIterator = iterator;
			this.counter = count;
		}

		public boolean hasNext() {
			return counter > 0 && innerIterator.hasNext();
		}

		public Object next() {
			if (counter > 0) {
				--counter;
				return innerIterator.next();
			}
			throw new NoSuchElementException();
		}

		public void remove() {
			innerIterator.remove();
		}
	}

	static final String OPERATOR = "limit"; //$NON-NLS-1$

	public Limit(Expression operand, Expression param) {
		super(operand, param);
	}

	public Limit(Expression operand, int limit) {
		this(operand, new Constant(new Integer(limit)));
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		Object rval = rhs.evaluate(context, scope);
		int limit = -1;
		if (rval instanceof Integer)
			limit = ((Integer) rval).intValue();
		if (limit <= 0)
			throw new IllegalArgumentException("limit expression did not evalutate to a positive integer"); //$NON-NLS-1$
		return new CountingIterator(lhs.evaluateAsIterator(context, scope), limit);
	}

	public void toString(StringBuffer bld) {
		CollectionFilter.appendProlog(bld, lhs, getOperator());
		appendOperand(bld, rhs, ExpressionParser.PRIORITY_COMMA);
		bld.append(')');
	}

	String getOperator() {
		return OPERATOR;
	}

	int getPriority() {
		return ExpressionParser.PRIORITY_COLLECTION;
	}
}
