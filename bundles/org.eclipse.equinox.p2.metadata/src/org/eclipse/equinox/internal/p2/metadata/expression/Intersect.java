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

import java.util.*;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 * n-ary <code>intersect</code> operator. The result is the set of elements that were found in all operands. 
 */
final class Intersect extends Binary {
	Intersect(Expression operand, Expression param) {
		super(operand, param);
	}

	public Object evaluate(IEvaluationContext context) {
		return evaluateAsIterator(context);
	}

	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		Set<?> resultSet = asSet(lhs.evaluate(context), false); // Safe since it will not be modified
		Iterator<?> itor = rhs.evaluateAsIterator(context);
		Set<Object> retained = new HashSet<Object>();
		while (itor.hasNext()) {
			Object value = itor.next();
			if (resultSet.contains(value))
				retained.add(value);
		}
		return RepeatableIterator.create(retained);
	}

	public int getExpressionType() {
		return TYPE_INTERSECT;
	}

	public String getOperator() {
		return KEYWORD_INTERSECT;
	}

	public int getPriority() {
		return PRIORITY_COLLECTION;
	}
}
