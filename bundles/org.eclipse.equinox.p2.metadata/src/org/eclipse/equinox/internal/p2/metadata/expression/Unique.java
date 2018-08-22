/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

		@Override
		protected boolean isMatch(T val) {
			synchronized (uniqueSet) {
				return uniqueSet.add(val);
			}
		}
	}

	Unique(Expression collection, Expression explicitCache) {
		super(collection, explicitCache);
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		return evaluateAsIterator(context);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		Object explicitCache = rhs.evaluate(context);
		Set<Object> uniqueSet;
		if (explicitCache == null)
			// No cache, we just ensure that the iteration is unique
			uniqueSet = new HashSet<>();
		else {
			if (!(explicitCache instanceof Set<?>))
				throw new IllegalArgumentException("Unique cache must be a java.util.Set"); //$NON-NLS-1$
			uniqueSet = (Set<Object>) explicitCache;
		}
		return new UniqueIterator<>(lhs.evaluateAsIterator(context), uniqueSet);
	}

	@Override
	public int getExpressionType() {
		return TYPE_UNIQUE;
	}

	@Override
	public void toString(StringBuffer bld, Variable rootVariable) {
		CollectionFilter.appendProlog(bld, rootVariable, lhs, getOperator());
		if (rhs != Literal.NULL_CONSTANT)
			appendOperand(bld, rootVariable, rhs, IExpressionConstants.PRIORITY_COMMA);
		bld.append(')');
	}

	@Override
	public String getOperator() {
		return KEYWORD_UNIQUE;
	}

	@Override
	public int getPriority() {
		return PRIORITY_COLLECTION;
	}
}
