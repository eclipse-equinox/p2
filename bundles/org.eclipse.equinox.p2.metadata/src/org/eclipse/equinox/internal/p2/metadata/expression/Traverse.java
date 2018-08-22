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

import java.util.*;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 * An expression that will collect items recursively based on a <code>rule</code>.
 * The <code>rule</code> is applied for each item in the <code>collection</code> and
 * is supposed to create a new collection. The <code>rule</code> is then applied for each item
 * in the new collection. All items are collected into a set and items that are already
 * in that set will not be perused again. The set becomes the result of the traversal.
 */
final class Traverse extends CollectionFilter {

	Traverse(Expression collection, LambdaExpression lambda) {
		super(collection, lambda);
	}

	@Override
	public Object evaluate(IEvaluationContext context, Iterator<?> itor) {
		return evaluateAsIterator(context, itor);
	}

	@Override
	public Iterator<?> evaluateAsIterator(IEvaluationContext context, Iterator<?> iterator) {
		HashSet<Object> collector = new HashSet<>();
		while (iterator.hasNext())
			traverse(collector, iterator.next(), context);
		return collector.iterator();
	}

	@Override
	public int getExpressionType() {
		return TYPE_TRAVERSE;
	}

	@Override
	public String getOperator() {
		return KEYWORD_TRAVERSE;
	}

	void traverse(Set<Object> collector, Object parent, IEvaluationContext context) {
		if (collector.add(parent)) {
			Variable variable = lambda.getItemVariable();
			context = EvaluationContext.create(context, variable);
			variable.setValue(context, parent);
			Iterator<?> subIterator = lambda.evaluateAsIterator(context);
			while (subIterator.hasNext())
				traverse(collector, subIterator.next(), context);
		}
	}
}
