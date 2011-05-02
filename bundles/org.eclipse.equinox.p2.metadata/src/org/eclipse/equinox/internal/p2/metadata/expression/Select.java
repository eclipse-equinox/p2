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
 * An expression that yields a new collection consisting of all elements of the
 * <code>collection</code> for which the <code>filter</code> yields <code>true</code>.
 */
final class Select extends CollectionFilter {
	Select(Expression collection, LambdaExpression lambda) {
		super(collection, lambda);
	}

	protected Object evaluate(IEvaluationContext context, Iterator<?> itor) {
		return evaluateAsIterator(context, itor);
	}

	protected Iterator<?> evaluateAsIterator(final IEvaluationContext context, Iterator<?> itor) {
		return new MatchIteratorFilter<Object>(itor) {
			protected boolean isMatch(Object val) {
				lambda.getItemVariable().setValue(context, val);
				return lambda.evaluate(context) == Boolean.TRUE;
			}
		};
	}

	public int getExpressionType() {
		return TYPE_SELECT;
	}

	public String getOperator() {
		return KEYWORD_SELECT;
	}

	boolean isCollection() {
		return true;
	}
}
