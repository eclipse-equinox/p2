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
public class LambdaExpression extends Unary {
	protected final Variable each;

	protected LambdaExpression(Variable each, Expression body) {
		super(body);
		this.each = each;
	}

	public boolean accept(IExpressionVisitor visitor) {
		return super.accept(visitor) && each.accept(visitor);
	}

	public int compareTo(Expression e) {
		int cmp = super.compareTo(e);
		if (cmp == 0)
			cmp = each.compareTo(((LambdaExpression) e).each);
		return cmp;
	}

	public boolean equals(Object o) {
		return super.equals(o) && each.equals(((LambdaExpression) o).each);
	}

	public int hashCode() {
		int result = 31 + operand.hashCode();
		return 31 * result + each.hashCode();
	}

	public int getExpressionType() {
		return TYPE_LAMBDA;
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		each.toString(bld, rootVariable);
		bld.append(" | "); //$NON-NLS-1$
		appendOperand(bld, rootVariable, operand, IExpressionConstants.PRIORITY_COMMA);
	}

	public Variable getItemVariable() {
		return each;
	}

	public String getOperator() {
		return "|"; //$NON-NLS-1$
	}

	public int getPriority() {
		return IExpressionConstants.PRIORITY_LAMBDA;
	}

	public IEvaluationContext prolog(IEvaluationContext context) {
		return EvaluationContext.create(context, each);
	}

	int countAccessToEverything() {
		return 2 * super.countAccessToEverything();
	}
}
