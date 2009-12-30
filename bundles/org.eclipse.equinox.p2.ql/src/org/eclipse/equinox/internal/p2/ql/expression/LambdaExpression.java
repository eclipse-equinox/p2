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

import org.eclipse.equinox.internal.p2.ql.MultiVariableContext;
import org.eclipse.equinox.internal.p2.ql.SingleVariableContext;
import org.eclipse.equinox.p2.ql.IEvaluationContext;
import org.eclipse.equinox.p2.ql.IExpressionVisitor;

/**
 * A function that executes some code
 */
final class LambdaExpression extends Unary {
	private static final Assignment[] emptyAssignmentArray = new Assignment[0];
	private final Assignment[] assignments;
	private final Variable each;

	LambdaExpression(Variable each, Expression body, Assignment[] assignments) {
		super(body);
		this.each = each;
		if (assignments == null)
			assignments = emptyAssignmentArray;
		this.assignments = assignments;
	}

	LambdaExpression(Variable variable, Expression body) {
		this(variable, body, null);
	}

	public boolean accept(IExpressionVisitor visitor) {
		if (super.accept(visitor) && each.accept(visitor)) {
			for (int idx = 0; idx < assignments.length; ++idx)
				if (!assignments[idx].accept(visitor))
					return false;
			return true;
		}
		return false;
	}

	public int getExpressionType() {
		return TYPE_LAMBDA;
	}

	public void toString(StringBuffer bld) {
		int top = assignments.length;
		if (top > 0) {
			for (int idx = 0; idx < top; ++idx) {
				appendOperand(bld, assignments[idx].rhs, PRIORITY_COMMA);
				bld.append(", "); //$NON-NLS-1$
			}
			bld.append(OPERATOR_EACH);
			bld.append(", {"); //$NON-NLS-1$
			for (int idx = 0; idx < top; ++idx) {
				appendOperand(bld, assignments[idx].lhs, PRIORITY_COMMA);
				bld.append(", "); //$NON-NLS-1$
			}
		}
		each.toString(bld);
		bld.append(" | "); //$NON-NLS-1$
		appendOperand(bld, operand, PRIORITY_COMMA);
		if (top > 0)
			bld.append('}');
	}

	int countReferenceToEverything() {
		if (super.countReferenceToEverything() > 0)
			return 2;
		for (int idx = 0; idx < assignments.length; ++idx)
			if (assignments[idx].countReferenceToEverything() > 0)
				return 2;
		return 0;
	}

	Variable getItemVariable() {
		return each;
	}

	String getOperator() {
		return "|"; //$NON-NLS-1$
	}

	int getPriority() {
		return PRIORITY_LAMBDA;
	}

	boolean isBoolean() {
		return operand.isBoolean();
	}

	boolean isCollection() {
		return operand.isCollection();
	}

	IEvaluationContext prolog(IEvaluationContext context) {
		IEvaluationContext lambdaContext = new SingleVariableContext(context, each);
		int top = assignments.length;
		if (top > 0) {
			if (top == 1) {
				Assignment v = assignments[0];
				lambdaContext = new SingleVariableContext(lambdaContext, v.lhs);
				lambdaContext.setValue(v.lhs, v.rhs.evaluate(context));
			} else {
				Variable[] vars = new Variable[top];
				for (int idx = 0; idx < top; ++idx)
					vars[idx] = (Variable) assignments[idx].lhs;
				lambdaContext = new MultiVariableContext(lambdaContext, vars);
				for (int idx = 0; idx < top; ++idx)
					lambdaContext.setValue(vars[idx], assignments[idx].rhs.evaluate(context));
			}
		}
		return lambdaContext;
	}
}
