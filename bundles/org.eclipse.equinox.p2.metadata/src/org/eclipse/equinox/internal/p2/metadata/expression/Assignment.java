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
 * An expression representing a variable stack in the current thread.
 */
class Assignment extends Binary {
	Assignment(Variable variable, Expression expression) {
		super(variable, expression);
	}

	@Override
	public final Object evaluate(IEvaluationContext context) {
		Object value = rhs.evaluate(context);
		context.setValue(lhs, value);
		return value;
	}

	@Override
	public int getExpressionType() {
		return TYPE_ASSIGNMENT;
	}

	@Override
	public int getPriority() {
		return IExpressionConstants.PRIORITY_ASSIGNMENT;
	}

	@Override
	public String getOperator() {
		return OPERATOR_ASSIGN;
	}

	@Override
	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		Iterator<?> value = rhs.evaluateAsIterator(context);
		context.setValue(lhs, value);
		return value;
	}
}
