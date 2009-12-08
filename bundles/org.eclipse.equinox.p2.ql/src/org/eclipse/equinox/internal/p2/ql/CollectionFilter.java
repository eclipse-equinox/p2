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

import java.util.Iterator;

/**
 * Some kind of operation that is performed for each element of a collection. I.e.
 * <code>x.&lt;operation&gt;(y | &lt;expression&rt;)</code>
 */
abstract class CollectionFilter extends Unary {
	static void appendProlog(StringBuffer bld, Expression lhs, String operator) {
		if (lhs != Variable.EVERYTHING && lhs != Variable.ITEM) {
			appendOperand(bld, lhs, ExpressionParser.PRIORITY_COLLECTION);
			bld.append('.');
		}
		bld.append(operator);
		bld.append('(');
	}

	final LambdaExpression lambda;
	final EachVariable variable;

	CollectionFilter(Expression collection, LambdaExpression lambda) {
		super(collection);
		this.lambda = lambda;
		this.variable = lambda.getItemVariable();
	}

	public boolean accept(Visitor visitor) {
		return super.accept(visitor) && lambda.accept(visitor);
	}

	public final Object evaluate(ExpressionContext context, VariableScope scope) {
		Iterator lval = operand.evaluateAsIterator(context, scope);
		scope = lambda.prolog(context, scope);
		return evaluate(context, scope, lval);
	}

	public void toString(StringBuffer bld) {
		appendProlog(bld, operand, getOperator());
		appendOperand(bld, lambda, ExpressionParser.PRIORITY_LAMBDA);
		bld.append(')');
	}

	int countReferenceToEverything() {
		return super.countReferenceToEverything() + lambda.countReferenceToEverything();
	}

	abstract Object evaluate(final ExpressionContext context, final VariableScope scope, Iterator iterator);

	int getPriority() {
		return ExpressionParser.PRIORITY_COLLECTION;
	}
}
