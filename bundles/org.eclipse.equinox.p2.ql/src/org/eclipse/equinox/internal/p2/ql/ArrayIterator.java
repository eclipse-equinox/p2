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
import java.util.NoSuchElementException;

/**
 * A MatchIteratorFilter controlled by an expression
 */
public class ArrayIterator implements Iterator {
	private final Expression[] expressions;

	private final ExpressionContext context;

	private final VariableScope scope;

	private int pos = -1;

	public ArrayIterator(ExpressionContext context, VariableScope scope, Expression[] expressions) {
		this.expressions = expressions;
		this.context = context;
		this.scope = scope;
	}

	public boolean hasNext() {
		return pos + 1 < expressions.length;
	}

	public Object next() {
		if (++pos >= expressions.length) {
			--pos;
			throw new NoSuchElementException();
		}
		return expressions[pos].evaluate(context, scope);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}