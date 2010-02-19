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
import org.eclipse.equinox.internal.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.expression.IExpressionVisitor;
import org.eclipse.equinox.p2.query.IQueryResult;

/**
 * The base class of the expression tree.
 */
public abstract class QLUtil implements IExpression, IQLConstants {

	static Set<?> asSet(Object val, boolean forcePrivateCopy) {
		if (val == null)
			throw new IllegalArgumentException("Cannot convert null into an set"); //$NON-NLS-1$

		if (val instanceof IRepeatableIterator<?>) {
			Object provider = ((IRepeatableIterator<?>) val).getIteratorProvider();
			if (!forcePrivateCopy) {
				if (provider instanceof Set<?>)
					return (Set<?>) provider;
				if (provider instanceof IQueryResult<?>)
					return ((IQueryResult<?>) provider).unmodifiableSet();
			}

			if (provider instanceof Collection<?>)
				val = provider;
		} else {
			if (!forcePrivateCopy) {
				if (val instanceof Set<?>)
					return (Set<?>) val;
				if (val instanceof IQueryResult<?>)
					return ((IQueryResult<?>) val).unmodifiableSet();
			}
		}

		HashSet<Object> result;
		if (val instanceof Collection<?>)
			result = new HashSet<Object>((Collection<?>) val);
		else {
			result = new HashSet<Object>();
			Iterator<?> iterator = RepeatableIterator.create(val);
			while (iterator.hasNext())
				result.add(iterator.next());
		}
		return result;
	}

	private static class TranslationSupportFinder implements IExpressionVisitor {
		private boolean found;

		TranslationSupportFinder() { //
		}

		public boolean visit(IExpression expression) {
			if (expression.getExpressionType() == TYPE_MEMBER && VARIABLE_TRANSLATIONS.equals(((Member) expression).getName()))
				found = true;
			return !found;
		}

		boolean isFound() {
			return found;
		}
	}

	/**
	 * Checks if the expression will make repeated requests for the 'everything' iterator.
	 * @return <code>true</code> if repeated requests will be made, <code>false</code> if not.
	 */
	public static boolean needsTranslationSupport(IExpression expression) {
		TranslationSupportFinder tsFinder = new TranslationSupportFinder();
		((Expression) expression).accept(tsFinder);
		return tsFinder.isFound();
	}
}
