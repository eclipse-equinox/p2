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

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IExpression;

/**
 */
final class Collect extends CollectionFilter {
	final class CollectIterator implements Iterator<Object> {
		private final IEvaluationContext context;

		private final IExpression variable;

		private final Iterator<?> innerIterator;

		public CollectIterator(IEvaluationContext context, Iterator<?> iterator) {
			this.context = context;
			this.variable = lambda.getItemVariable();
			this.innerIterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return innerIterator.hasNext();
		}

		@Override
		public Object next() {
			context.setValue(variable, innerIterator.next());
			return lambda.evaluate(context);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	Collect(Expression collection, LambdaExpression lambda) {
		super(collection, lambda);
	}

	@Override
	public Object evaluate(IEvaluationContext context, Iterator<?> itor) {
		return evaluateAsIterator(context, itor);
	}

	@Override
	public Iterator<?> evaluateAsIterator(IEvaluationContext context, Iterator<?> itor) {
		return new CollectIterator(context, itor);
	}

	@Override
	public int getExpressionType() {
		return TYPE_COLLECT;
	}

	@Override
	public String getOperator() {
		return KEYWORD_COLLECT;
	}
}
