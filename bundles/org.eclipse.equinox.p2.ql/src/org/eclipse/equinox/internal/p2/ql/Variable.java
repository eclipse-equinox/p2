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

import java.util.Iterator;

/**
 * An expression representing a variable stack in the current thread.
 */
public class Variable extends Expression {

	static final String KEYWORD_EVERYTHING = "everything"; //$NON-NLS-1$

	static final String KEYWORD_ITEM = "item"; //$NON-NLS-1$

	public static final Variable EVERYTHING = new Variable(KEYWORD_EVERYTHING);

	public static final Variable ITEM = new ItemVariable(KEYWORD_ITEM);

	private final String name;

	public static Variable createEach(String name) {
		return new EachVariable(name);
	}

	public static Variable create(String name) {
		if (EachVariable.KEYWORD_EACH.equals(name))
			return new EachVariable(name);
		if (KEYWORD_ITEM.equals(name))
			return ITEM;
		if (KEYWORD_EVERYTHING.equals(name))
			return EVERYTHING;
		return new Variable(name);
	}

	Variable(String name) {
		this.name = name;
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		return scope.getValue(this);
	}

	public Iterator evaluateAsIterator(ExpressionContext context, VariableScope scope) {
		Object value = evaluate(context, scope);
		if (value instanceof IRepeatableIterator)
			return ((IRepeatableIterator) value).getCopy();

		Iterator itor = RepeatableIterator.create(value);
		setValue(scope, itor);
		return itor;
	}

	public String getName() {
		return name;
	}

	public void toString(StringBuffer bld) {
		bld.append(name);
	}

	int countReferenceToEverything() {
		return KEYWORD_EVERYTHING.equals(name) ? 1 : 0;
	}

	int getPriority() {
		return ExpressionParser.PRIORITY_VARIABLE;
	}

	void setValue(VariableScope scope, Object value) {
		scope.setValue(this, value);
	}
}
