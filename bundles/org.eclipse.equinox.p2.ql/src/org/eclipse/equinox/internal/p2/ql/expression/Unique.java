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

import java.util.*;
import org.eclipse.equinox.internal.p2.ql.MatchIteratorFilter;
import org.eclipse.equinox.p2.ql.IEvaluationContext;

/**
 * An expression that ensures that the elements of its collection is only returned
 * once throughout the whole query.
 */
final class Unique extends Binary {
	static class UniqueIterator<T> extends MatchIteratorFilter<T> {
		private final Set<T> uniqueSet;

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
		assertNotBoolean(collection, "collection"); //$NON-NLS-1$
		assertNotBoolean(explicitCache, "cache"); //$NON-NLS-1$
	}

	@SuppressWarnings("unchecked")
	public Object evaluate(IEvaluationContext context) {
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

	public void toString(StringBuffer bld) {
		CollectionFilter.appendProlog(bld, lhs, getOperator());
		if (rhs != Constant.NULL_CONSTANT)
			appendOperand(bld, rhs, PRIORITY_COMMA);
		bld.append(')');
	}

	String getOperator() {
		return KEYWORD_UNIQUE;
	}

	int getPriority() {
		return PRIORITY_COLLECTION;
	}

	boolean isCollection() {
		return true;
	}

	boolean isElementBoolean() {
		return lhs.isElementBoolean();
	}

}
