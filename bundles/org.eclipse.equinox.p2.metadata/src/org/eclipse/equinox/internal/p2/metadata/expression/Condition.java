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
 * Comparisons for magnitude.
 */
final class Condition extends Binary {
	final Expression ifFalse;

	Condition(Expression test, Expression ifTrue, Expression ifFalse) {
		super(test, ifTrue);
		this.ifFalse = ifFalse;
	}

	public boolean equals(Object o) {
		return super.equals(o) && ifFalse.equals(((Condition) o).ifFalse);
	}

	public Object evaluate(IEvaluationContext context) {
		return lhs.evaluate(context) == Boolean.TRUE ? rhs.evaluate(context) : ifFalse.evaluate(context);
	}

	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		return lhs.evaluate(context) == Boolean.TRUE ? rhs.evaluateAsIterator(context) : ifFalse.evaluateAsIterator(context);
	}

	public int hashCode() {
		return super.hashCode() * 31 + ifFalse.hashCode();
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		super.toString(bld, rootVariable);
		bld.append(' ');
		bld.append(OPERATOR_ELSE);
		bld.append(' ');
		appendOperand(bld, rootVariable, ifFalse, getPriority());
	}

	public int getExpressionType() {
		return TYPE_CONDITION;
	}

	public String getOperator() {
		return OPERATOR_IF;
	}

	public int getPriority() {
		return IExpressionConstants.PRIORITY_CONDITION;
	}
}
