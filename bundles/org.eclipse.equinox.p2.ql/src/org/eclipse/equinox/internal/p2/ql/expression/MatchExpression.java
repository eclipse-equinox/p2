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

import org.eclipse.equinox.internal.p2.ql.*;
import org.eclipse.equinox.p2.ql.*;

/**
 * The match expression is the top expression in item queries. It introduces the
 * variable 'item' and initializes it with the item to match.
 */
final class MatchExpression extends Unary implements IMatchExpression {

	MatchExpression(Expression expression) {
		super(expression);
	}

	public boolean isMatch(IEvaluationContext context, Object value) {
		Variable.ITEM.setValue(context, value);
		return operand.evaluate(context) == Boolean.TRUE;
	}

	public IEvaluationContext createContext(Object[] params) {
		return new SingleVariableContext(new ParameterContext(params), Variable.ITEM);
	}

	public IEvaluationContext createContext(Object[] params, ITranslationSupport ts) {
		IEvaluationContext context = new MultiVariableContext(new ParameterContext(params), new IExpression[] {Variable.ITEM, Variable.TRANSLATIONS});
		context.setValue(Variable.TRANSLATIONS, ts);
		return context;
	}

	public int getExpressionType() {
		return operand.getExpressionType();
	}

	public void toString(StringBuffer bld) {
		operand.toString(bld);
	}

	protected int getPriority() {
		return operand.getPriority();
	}

	String getOperator() {
		throw new UnsupportedOperationException();
	}

	Expression pipeFrom(Expression expr) {
		return new MatchExpression(operand.pipeFrom(expr));
	}

	boolean isBoolean() {
		return true;
	}
}
