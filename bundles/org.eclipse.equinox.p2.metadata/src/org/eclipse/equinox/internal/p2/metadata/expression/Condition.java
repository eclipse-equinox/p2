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

/**
 * Comparisons for magnitude.
 */
final class Condition extends Binary {
	final Expression ifFalse;

	Condition(Expression test, Expression ifTrue, Expression ifFalse) {
		super(test, ifTrue);
		this.ifFalse = ifFalse;
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o) && ifFalse.equals(((Condition) o).ifFalse);
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		return lhs.evaluate(context) == Boolean.TRUE ? rhs.evaluate(context) : ifFalse.evaluate(context);
	}

	@Override
	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		return lhs.evaluate(context) == Boolean.TRUE ? rhs.evaluateAsIterator(context) : ifFalse.evaluateAsIterator(context);
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 31 + ifFalse.hashCode();
	}

	@Override
	public void toString(StringBuffer bld, Variable rootVariable) {
		super.toString(bld, rootVariable);
		bld.append(' ');
		bld.append(OPERATOR_ELSE);
		bld.append(' ');
		appendOperand(bld, rootVariable, ifFalse, getPriority());
	}

	@Override
	public int getExpressionType() {
		return TYPE_CONDITION;
	}

	@Override
	public String getOperator() {
		return OPERATOR_IF;
	}

	@Override
	public int getPriority() {
		return IExpressionConstants.PRIORITY_CONDITION;
	}
}
