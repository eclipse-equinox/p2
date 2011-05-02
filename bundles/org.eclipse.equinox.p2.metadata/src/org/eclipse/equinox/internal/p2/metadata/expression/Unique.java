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

import java.util.*;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 * An expression that ensures that the elements of its collection is only returned
 * once throughout the whole query.
 */
final class Unique extends Binary {
	/**
	 * A UniqueIterator that uses a set as a discriminator, ensuring that
	 * no element is returned twice.
	 */
	static class UniqueIterator<T> extends MatchIteratorFilter<T> {
		final Set<T> uniqueSet;

		public UniqueIterator(Iterator<? extends T> iterator, Set<T> uniqueSet) {
			super(iterator);
			this.uniqueSet = uniqueSet;
		}

		protected boolean isMatch(T val) {
			synchronized (uniqueSet) {
				return uniqueSet.add(val);
			}
		}
	}

	Unique(Expression collection, Expression explicitCache) {
		super(collection, explicitCache);
	}

	public Object evaluate(IEvaluationContext context) {
		return evaluateAsIterator(context);
	}

	@SuppressWarnings("unchecked")
	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		Object explicitCache = rhs.evaluate(context);
		Set<Object> uniqueSet;
		if (explicitCache == null)
			// No cache, we just ensure that the iteration is unique
			uniqueSet = new HashSet<Object>();
		else {
			if (!(explicitCache instanceof Set<?>))
				throw new IllegalArgumentException("Unique cache must be a java.util.Set"); //$NON-NLS-1$
			uniqueSet = (Set<Object>) explicitCache;
		}
		return new UniqueIterator<Object>(lhs.evaluateAsIterator(context), uniqueSet);
	}

	public int getExpressionType() {
		return TYPE_UNIQUE;
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		CollectionFilter.appendProlog(bld, rootVariable, lhs, getOperator());
		if (rhs != Literal.NULL_CONSTANT)
			appendOperand(bld, rootVariable, rhs, IExpressionConstants.PRIORITY_COMMA);
		bld.append(')');
	}

	public String getOperator() {
		return KEYWORD_UNIQUE;
	}

	public int getPriority() {
		return PRIORITY_COLLECTION;
	}
}
