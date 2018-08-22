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
import java.util.Set;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 */
final class Union extends Binary {
	Union(Expression operand, Expression param) {
		super(operand, param);
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		return evaluateAsIterator(context);
	}

	@Override
	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		@SuppressWarnings("unchecked")
		Set<Object> resultSet = (Set<Object>) asSet(lhs.evaluate(context), true);
		Iterator<?> itor = rhs.evaluateAsIterator(context);
		while (itor.hasNext())
			resultSet.add(itor.next());
		return RepeatableIterator.create(resultSet);
	}

	@Override
	public int getExpressionType() {
		return TYPE_UNION;
	}

	@Override
	public String getOperator() {
		return KEYWORD_UNION;
	}

	@Override
	public int getPriority() {
		return PRIORITY_COLLECTION;
	}
}
