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


/**
 * The context expression is the top expression in context queries. It introduces the
 * variable 'everything' and initialized it with the iterator that represents all
 * available items.
 */
public final class ContextExpression extends Binary {

	public ContextExpression(Variable contextVariable, Expression expression) {
		super(contextVariable, expression);
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		((Variable) lhs).setValue(scope, context);
		return rhs.evaluate(context, scope);
	}

	public void toString(StringBuffer bld) {
		rhs.toString(bld);
	}

	int countReferenceToEverything() {
		return rhs.countReferenceToEverything();
	}

	String getOperator() {
		throw new UnsupportedOperationException();
	}

	int getPriority() {
		return rhs.getPriority();
	}
}
