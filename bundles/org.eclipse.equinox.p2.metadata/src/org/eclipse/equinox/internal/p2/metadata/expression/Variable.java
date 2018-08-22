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
public class Variable extends Expression {

	private final String name;

	public Variable(String name) {
		this.name = name;
	}

	@Override
	public int compareTo(Expression e) {
		int cmp = super.compareTo(e);
		if (cmp == 0)
			cmp = name.compareTo(((Variable) e).name);
		return cmp;
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o) && name.equals(((Variable) o).name);
	}

	@Override
	public final Object evaluate(IEvaluationContext context) {
		return context.getValue(this);
	}

	@Override
	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		Object value = context.getValue(this);
		if (value instanceof IRepeatableIterator<?>)
			return ((IRepeatableIterator<?>) value).getCopy();

		Iterator<?> itor = RepeatableIterator.create(value);
		setValue(context, itor);
		return itor;
	}

	@Override
	public int getExpressionType() {
		return TYPE_VARIABLE;
	}

	public String getName() {
		return name;
	}

	@Override
	public String getOperator() {
		return "<variable>"; //$NON-NLS-1$
	}

	@Override
	public int getPriority() {
		return PRIORITY_VARIABLE;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public final void setValue(IEvaluationContext context, Object value) {
		context.setValue(this, value);
	}

	@Override
	public void toString(StringBuffer bld, Variable rootVariable) {
		bld.append(name);
	}

	@Override
	int countAccessToEverything() {
		return this == ExpressionFactory.EVERYTHING ? 1 : 0;
	}
}
