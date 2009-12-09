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

import java.util.*;

/**
 * An expression that will collect items recursively based on a <code>rule</code>.
 * The <code>rule</code> is applied for each item in the <code>collection</code> and
 * is supposed to create a new collection. The <code>rule</code> is then applied for each item
 * in the new collection. All items are collected into a set and items that are already
 * in that set will not be perused again. The set becomes the result of the traversal.
 */
public final class Traverse extends CollectionFilter {

	public static final String OPERATOR = "traverse"; //$NON-NLS-1$

	public Traverse(Expression collection, LambdaExpression lambda) {
		super(collection, lambda);
	}

	Object evaluate(ExpressionContext context, VariableScope scope, Iterator iterator) {
		HashSet collector = new HashSet();
		while (iterator.hasNext())
			traverse(collector, iterator.next(), context, scope);
		return collector;
	}

	String getOperator() {
		return OPERATOR;
	}

	void traverse(Set collector, Object parent, ExpressionContext context, VariableScope scope) {
		if (collector.add(parent)) {
			scope = new SingleVariableScope(scope, variable);
			variable.setValue(scope, parent);
			Iterator subIterator = lambda.evaluateAsIterator(context, scope);
			while (subIterator.hasNext())
				traverse(collector, subIterator.next(), context, scope);
		}
	}
}
