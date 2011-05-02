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

/**
 * The abstract base class for the indexed and keyed parameters
 */
public class Parameter extends Expression {
	final int position;

	Parameter(int position) {
		this.position = position;
	}

	public int compareTo(Expression e) {
		int cmp = super.compareTo(e);
		if (cmp == 0)
			cmp = position - ((Parameter) e).position;
		return cmp;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o == null)
			return false;
		return getClass() == o.getClass() && position == ((Parameter) o).position;
	}

	public Object evaluate(IEvaluationContext context) {
		return context.getParameter(position);
	}

	public int getExpressionType() {
		return TYPE_PARAMETER;
	}

	public String getOperator() {
		return OPERATOR_PARAMETER;
	}

	public int getPriority() {
		return PRIORITY_VARIABLE;
	}

	public int hashCode() {
		return 31 * (1 + position);
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		bld.append('$');
		bld.append(position);
	}
}
