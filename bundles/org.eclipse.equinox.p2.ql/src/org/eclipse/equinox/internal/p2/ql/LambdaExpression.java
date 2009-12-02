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
 * A function that executes some code
 */
public final class LambdaExpression extends Unary {
	private final Expression[] currying;
	private final Variable[] variables;

	public LambdaExpression(Expression body, Expression[] currying, Variable[] variables) {
		super(body);
		int idx = currying.length;
		if (idx > 0) {
			if (idx != variables.length)
				throw new IllegalArgumentException("Number of currying expressions and variables differ"); //$NON-NLS-1$
			int anyCount = 0;
			while (--idx >= 0)
				if (currying[idx] instanceof EachVariable)
					++anyCount;
			if (anyCount != 1)
				throw new IllegalArgumentException("Exaclty one _ must be present among the currying expressions"); //$NON-NLS-1$
		} else if (variables.length != 1)
			throw new IllegalArgumentException("Exaclty one variable required unless currying is used"); //$NON-NLS-1$
		this.currying = currying;
		this.variables = variables;
	}

	public LambdaExpression(Expression body, Variable variable) {
		this(body, Expression.emptyArray, new Variable[] {variable});
	}

	public boolean accept(Visitor visitor) {
		if (super.accept(visitor)) {
			for (int idx = 0; idx < currying.length; ++idx)
				if (!currying[idx].accept(visitor))
					return false;
			for (int idx = 0; idx < variables.length; ++idx)
				if (!variables[idx].accept(visitor))
					return false;
		}
		return true;
	}

	public void toString(StringBuffer bld) {
		int top = currying.length;
		if (top > 0) {
			Array.elementsToString(bld, currying);
			bld.append(", {"); //$NON-NLS-1$
		}
		Array.elementsToString(bld, variables);
		bld.append(" | "); //$NON-NLS-1$
		appendOperand(bld, operand, ExpressionParser.PRIORITY_COMMA);
		if (top > 0)
			bld.append('}');
	}

	int countReferenceToEverything() {
		if (super.countReferenceToEverything() > 0)
			return 2;
		for (int idx = 0; idx < currying.length; ++idx)
			if (currying[idx].countReferenceToEverything() > 0)
				return 2;
		for (int idx = 0; idx < variables.length; ++idx)
			if (variables[idx].countReferenceToEverything() > 0)
				return 2;
		return 0;
	}

	EachVariable getItemVariable() {
		int idx = currying.length;
		if (idx == 0)
			// No currying. Then we know that we have exactly one variable
			return (EachVariable) variables[0];

		while (--idx >= 0) {
			Expression expr = currying[idx];
			if (expr instanceof EachVariable)
				return (EachVariable) variables[idx];
		}
		return null;
	}

	String getOperator() {
		return "|"; //$NON-NLS-1$
	}

	int getPriority() {
		return ExpressionParser.PRIORITY_LAMBDA;
	}

	VariableScope prolog(ExpressionContext context, VariableScope scope) {
		VariableScope lambdaScope = new VariableScope(scope);
		for (int idx = 0; idx < currying.length; ++idx) {
			Expression curry = currying[idx];
			if (!(curry instanceof EachVariable))
				variables[idx].setValue(lambdaScope, curry.evaluate(context, scope));
		}
		return lambdaScope;
	}
}
