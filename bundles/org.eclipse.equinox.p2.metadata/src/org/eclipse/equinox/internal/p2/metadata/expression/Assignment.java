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
 * An expression representing a variable stack in the current thread.
 */
class Assignment extends Binary {
	Assignment(Variable variable, Expression expression) {
		super(variable, expression);
	}

	public final Object evaluate(IEvaluationContext context) {
		Object value = rhs.evaluate(context);
		context.setValue(lhs, value);
		return value;
	}

	public int getExpressionType() {
		return TYPE_ASSIGNMENT;
	}

	public int getPriority() {
		return IExpressionConstants.PRIORITY_ASSIGNMENT;
	}

	public String getOperator() {
		return OPERATOR_ASSIGN;
	}

	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		Iterator<?> value = rhs.evaluateAsIterator(context);
		context.setValue(lhs, value);
		return value;
	}
}
