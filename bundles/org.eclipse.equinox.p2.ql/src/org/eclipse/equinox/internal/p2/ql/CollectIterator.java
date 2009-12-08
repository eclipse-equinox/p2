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
 * A MatchIteratorFilter controlled by an expression
 */
public class CollectIterator implements Iterator {
	private final Expression expression;

	private final ExpressionContext context;

	private final VariableScope scope;

	private final EachVariable variable;

	private final Iterator innerIterator;

	public CollectIterator(ExpressionContext context, VariableScope scope, EachVariable variable, Iterator iterator, Expression expression) {
		this.expression = expression;
		this.context = context;
		this.scope = scope;
		this.variable = variable;
		this.innerIterator = iterator;
	}

	public boolean hasNext() {
		return innerIterator.hasNext();
	}

	public Object next() {
		variable.setValue(scope, innerIterator.next());
		return expression.evaluate(context, scope);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}