/*******************************************************************************
 * Copyright (c) 2009, 2016 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.util.*;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 * A collection filter that limits the number of entries in the collection
 */
final class Limit extends Binary {

	/**
	 * An iterator that stops iterating after a given number of iterations.
	 */
	static final class CountingIterator<T> implements Iterator<T> {
		private final Iterator<? extends T> innerIterator;
		private int counter;

		public CountingIterator(Iterator<? extends T> iterator, int count) {
			this.innerIterator = iterator;
			this.counter = count;
		}

		public boolean hasNext() {
			return counter > 0 && innerIterator.hasNext();
		}

		public T next() {
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

	Limit(Expression operand, Expression param) {
		super(operand, param);
	}

	Limit(Expression operand, int limit) {
		this(operand, (Expression) ExpressionFactory.INSTANCE.constant(Integer.valueOf(limit)));
	}

	public Object evaluate(IEvaluationContext context) {
		return evaluateAsIterator(context);
	}

	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		Object rval = rhs.evaluate(context);
		int limit = -1;
		if (rval instanceof Integer)
			limit = ((Integer) rval).intValue();
		if (limit < 0)
			throw new IllegalArgumentException("limit expression did not evalutate to a positive integer"); //$NON-NLS-1$
		return limit == 0 ? Collections.emptySet().iterator() : new CountingIterator<Object>(lhs.evaluateAsIterator(context), limit);
	}

	public int getExpressionType() {
		return TYPE_LIMIT;
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		CollectionFilter.appendProlog(bld, rootVariable, lhs, getOperator());
		appendOperand(bld, rootVariable, rhs, IExpressionConstants.PRIORITY_COMMA);
		bld.append(')');
	}

	public String getOperator() {
		return KEYWORD_LIMIT;
	}

	public int getPriority() {
		return PRIORITY_COLLECTION;
	}
}
