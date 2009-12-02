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

import java.util.*;

/**
 * An expression that ensures that the elements of its collection is only returned
 * once throughout the whole query.
 */
public final class Unique extends Binary {
	public static class UniqueIterator extends MatchIteratorFilter {
		private final Set uniqueSet;

		public UniqueIterator(Class instanceClass, Iterator iterator, Set uniqueSet) {
			super(instanceClass, iterator);
			this.uniqueSet = uniqueSet;
		}

		protected boolean isMatch(Object val) {
			synchronized (uniqueSet) {
				return uniqueSet.add(val);
			}
		}
	}

	static final String OPERATOR = "unique"; //$NON-NLS-1$

	public Unique(Expression collection, Expression cacheId) {
		super(collection, cacheId);
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		Object explicitCache = rhs.evaluate(context, scope);
		Set uniqueSet;
		if (explicitCache == null)
			// No cache, we just ensure that the iteration is unique
			uniqueSet = new HashSet();
		else {
			if (!(explicitCache instanceof Set))
				throw new IllegalArgumentException("Unique cache must be a java.util.Set"); //$NON-NLS-1$
			uniqueSet = (Set) explicitCache;
		}
		return new UniqueIterator(Object.class, lhs.evaluateAsIterator(context, scope), uniqueSet);
	}

	public void toString(StringBuffer bld) {
		CollectionFilter.appendProlog(bld, lhs, getOperator());
		if (rhs != Constant.NULL_CONSTANT)
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
