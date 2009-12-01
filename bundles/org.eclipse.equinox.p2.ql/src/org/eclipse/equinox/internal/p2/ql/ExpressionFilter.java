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
public class ExpressionFilter extends MatchIteratorFilter {
	private final ItemExpression expression;

	private final ExpressionContext context;

	private final VariableScope scope;

	public ExpressionFilter(ExpressionContext context, VariableScope scope, Iterator iterator, ItemExpression expression) {
		super(context.getInstanceClass(), iterator);
		this.expression = expression;
		this.context = context;
		this.scope = scope;
	}

	protected boolean isMatch(Object val) {
		return expression.isMatch(context, scope, val);
	}
}