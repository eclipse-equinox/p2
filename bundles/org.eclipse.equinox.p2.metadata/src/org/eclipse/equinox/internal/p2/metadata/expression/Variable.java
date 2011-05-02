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
public class Variable extends Expression {

	private final String name;

	public Variable(String name) {
		this.name = name;
	}

	public int compareTo(Expression e) {
		int cmp = super.compareTo(e);
		if (cmp == 0)
			cmp = name.compareTo(((Variable) e).name);
		return cmp;
	}

	public boolean equals(Object o) {
		return super.equals(o) && name.equals(((Variable) o).name);
	}

	public final Object evaluate(IEvaluationContext context) {
		return context.getValue(this);
	}

	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		Object value = context.getValue(this);
		if (value instanceof IRepeatableIterator<?>)
			return ((IRepeatableIterator<?>) value).getCopy();

		Iterator<?> itor = RepeatableIterator.create(value);
		setValue(context, itor);
		return itor;
	}

	public int getExpressionType() {
		return TYPE_VARIABLE;
	}

	public String getName() {
		return name;
	}

	public String getOperator() {
		return "<variable>"; //$NON-NLS-1$
	}

	public int getPriority() {
		return PRIORITY_VARIABLE;
	}

	public int hashCode() {
		return name.hashCode();
	}

	public final void setValue(IEvaluationContext context, Object value) {
		context.setValue(this, value);
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		bld.append(name);
	}

	int countAccessToEverything() {
		return this == ExpressionFactory.EVERYTHING ? 1 : 0;
	}
}
