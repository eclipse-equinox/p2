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

import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IExpressionVisitor;

/**
 * A function that executes some code
 */
final class CurryedLambdaExpression extends LambdaExpression {
	private static final Assignment[] emptyAssignmentArray = new Assignment[0];
	private final Assignment[] assignments;

	CurryedLambdaExpression(Variable each, Assignment[] assignments, Expression body) {
		super(each, body);
		if (assignments == null)
			assignments = emptyAssignmentArray;
		this.assignments = assignments;
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

	public int compareTo(Expression e) {
		int cmp = super.compareTo(e);
		if (cmp == 0)
			cmp = compare(assignments, ((CurryedLambdaExpression) e).assignments);
		return cmp;
	}

	public boolean equals(Object o) {
		return super.equals(o) && equals(assignments, ((CurryedLambdaExpression) o).assignments);
	}

	public int hashCode() {
		return 31 * super.hashCode() + hashCode(assignments);
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		int top = assignments.length;
		if (top > 0) {
			for (int idx = 0; idx < top; ++idx) {
				appendOperand(bld, rootVariable, assignments[idx].rhs, IExpressionConstants.PRIORITY_COMMA);
				bld.append(", "); //$NON-NLS-1$
			}
			bld.append(OPERATOR_EACH);
			bld.append(", {"); //$NON-NLS-1$
			for (int idx = 0; idx < top; ++idx) {
				appendOperand(bld, rootVariable, assignments[idx].lhs, IExpressionConstants.PRIORITY_COMMA);
				bld.append(", "); //$NON-NLS-1$
			}
		}
		super.toString(bld, rootVariable);
		if (top > 0)
			bld.append('}');
	}

	public IEvaluationContext prolog(IEvaluationContext context) {
		IEvaluationContext lambdaContext;
		int top = assignments.length + 1;
		Variable[] vars = new Variable[top];
		vars[0] = getItemVariable();
		for (int idx = 1; idx < top; ++idx)
			vars[idx] = (Variable) assignments[idx - 1].lhs;
		lambdaContext = EvaluationContext.create(context, vars);
		for (int idx = 1; idx < top; ++idx)
			lambdaContext.setValue(vars[idx], assignments[idx - 1].rhs.evaluate(context));
		return lambdaContext;
	}

	int countAccessToEverything() {
		int cnt = 0;
		for (int idx = 0; idx < assignments.length; ++idx)
			cnt += assignments[idx].countAccessToEverything();
		cnt += super.countAccessToEverything();
		return cnt;
	}
}
