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

import org.eclipse.equinox.p2.ql.IEvaluationContext;

/**
 * Comparisons for magnitude.
 */
final class Condition extends Binary {
	final Expression ifFalse;

	Condition(Expression test, Expression ifTrue, Expression ifFalse) {
		super(test, ifTrue);
		this.ifFalse = ifFalse;
		assertNotCollection(test, "test"); //$NON-NLS-1$
	}

	public Object evaluate(IEvaluationContext context) {
		return lhs.evaluate(context) == Boolean.TRUE ? rhs.evaluate(context) : ifFalse.evaluate(context);
	}

	public int getExpressionType() {
		return TYPE_CONDITION;
	}

	public void toString(StringBuffer bld) {
		super.toString(bld);
		bld.append(' ');
		bld.append(OPERATOR_ELSE);
		bld.append(' ');
		appendOperand(bld, ifFalse, getPriority());
	}

	String getOperator() {
		return OPERATOR_IF;
	}

	int getPriority() {
		return PRIORITY_CONDITION;
	}

	boolean isBoolean() {
		return rhs.isBoolean() && ifFalse.isBoolean();
	}

	boolean isCollection() {
		return rhs.isCollection() && ifFalse.isCollection();
	}
}
