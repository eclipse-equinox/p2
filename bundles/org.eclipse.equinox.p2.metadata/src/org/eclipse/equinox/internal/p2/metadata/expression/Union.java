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
import java.util.Set;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 */
final class Union extends Binary {
	Union(Expression operand, Expression param) {
		super(operand, param);
	}

	public Object evaluate(IEvaluationContext context) {
		return evaluateAsIterator(context);
	}

	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		@SuppressWarnings("unchecked")
		Set<Object> resultSet = (Set<Object>) asSet(lhs.evaluate(context), true);
		Iterator<?> itor = rhs.evaluateAsIterator(context);
		while (itor.hasNext())
			resultSet.add(itor.next());
		return RepeatableIterator.create(resultSet);
	}

	public int getExpressionType() {
		return TYPE_UNION;
	}

	public String getOperator() {
		return KEYWORD_UNION;
	}

	public int getPriority() {
		return PRIORITY_COLLECTION;
	}
}
