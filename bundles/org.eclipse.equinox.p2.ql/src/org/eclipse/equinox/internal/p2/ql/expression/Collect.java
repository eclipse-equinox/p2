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

import java.util.Iterator;
import org.eclipse.equinox.p2.ql.IEvaluationContext;
import org.eclipse.equinox.p2.ql.IExpression;

/**
 */
final class Collect extends CollectionFilter {
	final class CollectIterator implements Iterator {
		private final IEvaluationContext context;

		private final IExpression variable;

		private final Iterator innerIterator;

		public CollectIterator(IEvaluationContext context, Iterator iterator) {
			this.context = context;
			this.variable = lambda.getItemVariable();
			this.innerIterator = iterator;
		}

		public boolean hasNext() {
			return innerIterator.hasNext();
		}

		public Object next() {
			context.setValue(variable, innerIterator.next());
			return lambda.evaluate(context);
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	Collect(Expression collection, LambdaExpression lambda) {
		super(collection, lambda);
	}

	Object evaluate(IEvaluationContext context, Iterator itor) {
		return evaluateAsIterator(context, itor);
	}

	Iterator evaluateAsIterator(IEvaluationContext context, Iterator itor) {
		return new CollectIterator(context, itor);
	}

	public int getExpressionType() {
		return TYPE_COLLECT;
	}

	String getOperator() {
		return KEYWORD_COLLECT;
	}

	boolean isCollection() {
		return true;
	}
}
