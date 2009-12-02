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
 * The item expression is the top expression in item queries. It introduces the
 * variable 'item' and initializes it with the item to match.
 */
public final class ItemExpression extends Binary {

	public ItemExpression(Variable itemVariable, Expression expression) {
		super(itemVariable, expression);
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		return rhs.evaluate(context, scope);
	}

	public boolean isMatch(ExpressionContext context, VariableScope scope, Object value) {
		scope.setItem(value);
		return rhs.evaluate(context, scope) == Boolean.TRUE;
	}

	public void toString(StringBuffer bld) {
		rhs.toString(bld);
	}

	protected int getPriority() {
		return rhs.getPriority();
	}

	Object evaluate(Object lval, Object rval) {
		throw new UnsupportedOperationException();
	}

	String getOperator() {
		throw new UnsupportedOperationException();
	}
}
