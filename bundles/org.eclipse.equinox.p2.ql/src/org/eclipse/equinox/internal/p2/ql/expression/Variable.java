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

import java.util.Iterator;
import org.eclipse.equinox.internal.p2.ql.IRepeatableIterator;
import org.eclipse.equinox.internal.p2.ql.RepeatableIterator;
import org.eclipse.equinox.p2.ql.IEvaluationContext;
import org.eclipse.equinox.p2.ql.IExpression;

/**
 * An expression representing a variable stack in the current thread.
 */
class Variable extends Expression {

	static final Variable EVERYTHING = new Variable(VARIABLE_EVERYTHING);

	static final Variable TRANSLATIONS = new Variable(VARIABLE_TRANSLATIONS);

	static final Variable ITEM = new Variable(VARIABLE_ITEM);

	private final String name;

	public static Variable create(String name) {
		if (VARIABLE_ITEM.equals(name))
			return ITEM;
		if (VARIABLE_EVERYTHING.equals(name))
			return EVERYTHING;
		if (VARIABLE_TRANSLATIONS.equals(name))
			return TRANSLATIONS;
		return new Variable(name);
	}

	Variable(String name) {
		this.name = name;
	}

	public final Object evaluate(IEvaluationContext context) {
		return context.getValue(this);
	}

	public Iterator evaluateAsIterator(IEvaluationContext context) {
		Object value = context.getValue(this);
		if (value instanceof IRepeatableIterator)
			return ((IRepeatableIterator) value).getCopy();

		Iterator itor = RepeatableIterator.create(value);
		setValue(context, itor);
		return itor;
	}

	public String getName() {
		return name;
	}

	public final void setValue(IEvaluationContext context, Object value) {
		context.setValue(this, value);
	}

	public void toString(StringBuffer bld) {
		bld.append(name);
	}

	public int getExpressionType() {
		return TYPE_VARIABLE;
	}

	int countReferenceToEverything() {
		return IExpression.VARIABLE_EVERYTHING.equals(name) ? 1 : 0;
	}

	int getPriority() {
		return PRIORITY_VARIABLE;
	}

	String getOperator() {
		return "<variable>"; //$NON-NLS-1$
	}
}
