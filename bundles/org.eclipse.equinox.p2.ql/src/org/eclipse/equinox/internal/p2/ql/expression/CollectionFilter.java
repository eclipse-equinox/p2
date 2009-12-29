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
import org.eclipse.equinox.p2.ql.IExpressionVisitor;

/**
 * Some kind of operation that is performed for each element of a collection. I.e.
 * <code>x.&lt;operation&gt;(y | &lt;expression&rt;)</code>
 */
abstract class CollectionFilter extends Unary {
	static void appendProlog(StringBuffer bld, Expression lhs, String operator) {
		if (lhs != Variable.EVERYTHING && lhs != Variable.ITEM) {
			appendOperand(bld, lhs, PRIORITY_COLLECTION);
			bld.append('.');
		}
		bld.append(operator);
		bld.append('(');
	}

	final LambdaExpression lambda;

	CollectionFilter(Expression collection, LambdaExpression lambda) {
		super(collection);
		this.lambda = lambda;
	}

	public boolean accept(IExpressionVisitor visitor) {
		return super.accept(visitor) && lambda.accept(visitor);
	}

	public final Object evaluate(IEvaluationContext context) {
		Iterator<?> lval = operand.evaluateAsIterator(context);
		context = lambda.prolog(context);
		return evaluate(context, lval);
	}

	public final Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		Iterator<?> lval = operand.evaluateAsIterator(context);
		context = lambda.prolog(context);
		return evaluateAsIterator(context, lval);
	}

	public void toString(StringBuffer bld) {
		appendProlog(bld, operand, getOperator());
		appendOperand(bld, lambda, PRIORITY_LAMBDA);
		bld.append(')');
	}

	int countReferenceToEverything() {
		return super.countReferenceToEverything() + lambda.countReferenceToEverything();
	}

	abstract Object evaluate(final IEvaluationContext context, Iterator<?> iterator);

	Iterator<?> evaluateAsIterator(IEvaluationContext context, Iterator<?> iterator) {
		throw new UnsupportedOperationException();
	}

	int getPriority() {
		return PRIORITY_COLLECTION;
	}
}
