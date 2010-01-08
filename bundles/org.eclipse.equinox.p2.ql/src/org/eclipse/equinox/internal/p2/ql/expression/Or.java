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
import java.util.Set;
import org.eclipse.equinox.p2.ql.IEvaluationContext;

/**
 * n-ary OR operator. This operator evaluates its first operand and then checks
 * the class of the result. If the class is {@link Boolean} then it is assumed
 * that all other operands also evaluates to a boolean and the full evaluation is
 * <code>false</code> if none of its operands evaluate to <code>true</code>. If the
 * first result was not of class {@link Boolean}, then it is assumed that it can
 * be accessed as an {@link Iterator} and that all other operands also evaluates to
 * something that can be accessed an {@link Iterator}. The OR operator will then
 * function as a UNION operator and the result is the unique sum of all elements that
 * were found in all operands. 
 */
final class Or extends NAry {
	public Or(Expression[] operands) {
		super(assertLength(operands, 2, OPERATOR_OR));
	}

	public Object evaluate(IEvaluationContext context) {
		Object firstValue = operands[0].evaluate(context);

		// Determine operation mode
		if (firstValue instanceof Boolean) {
			// The first value was boolean. Assume that the rest are too
			if (((Boolean) firstValue).booleanValue())
				return Boolean.TRUE;

			for (int idx = 1; idx < operands.length; ++idx) {
				if (operands[idx].evaluate(context) == Boolean.TRUE)
					return Boolean.TRUE;
			}
			return Boolean.FALSE;
		}

		// Not a boolean. Assume that we can use an iterator on all values
		@SuppressWarnings("unchecked")
		Set<Object> resultSet = (Set<Object>) asSet(firstValue, true);
		for (int idx = 1; idx < operands.length; ++idx) {
			Iterator<?> itor = operands[idx].evaluateAsIterator(context);
			while (itor.hasNext())
				resultSet.add(itor.next());
		}
		return resultSet;
	}

	public int getExpressionType() {
		return TYPE_OR;
	}

	String getOperator() {
		return OPERATOR_OR;
	}

	int getPriority() {
		return PRIORITY_OR;
	}
}
