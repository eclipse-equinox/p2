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

import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 * The abstract base class for the indexed and keyed parameters
 */
public class Parameter extends Expression {
	final int position;

	Parameter(int position) {
		this.position = position;
	}

	@Override
	public int compareTo(Expression e) {
		int cmp = super.compareTo(e);
		if (cmp == 0)
			cmp = position - ((Parameter) e).position;
		return cmp;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o == null)
			return false;
		return getClass() == o.getClass() && position == ((Parameter) o).position;
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		return context.getParameter(position);
	}

	@Override
	public int getExpressionType() {
		return TYPE_PARAMETER;
	}

	@Override
	public String getOperator() {
		return OPERATOR_PARAMETER;
	}

	@Override
	public int getPriority() {
		return PRIORITY_VARIABLE;
	}

	@Override
	public int hashCode() {
		return 31 * (1 + position);
	}

	@Override
	public void toString(StringBuffer bld, Variable rootVariable) {
		bld.append('$');
		bld.append(position);
	}
}
