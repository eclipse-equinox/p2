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

import java.util.*;
import org.eclipse.equinox.p2.ql.IEvaluationContext;

/**
 * n-ary AND operator. This operator evaluates its first operand and then checks
 * the class of the result. If the class is {@link Boolean} then it is assumed
 * that all other operands also evaluates to a boolean and the full evaluation is
 * <code>true</code> if all its operands evaluate to <code>true</code>. If the first
 * result was not of class {@link Boolean}, then it is assumed that it can be accessed
 * as an {@link Iterator} and that all other operands also evaluates to something that
 * can be accessed as an {@link Iterator}. The AND operator will then function as a
 * INTERSECT operator and the result is the set of elements that were found in all operands. 
 */
final class And extends NAry {
	And(Expression[] operands) {
		super(assertLength(operands, 2, OPERATOR_AND));
	}

	public Object evaluate(IEvaluationContext context) {
		Object firstValue = operands[0].evaluate(context);

		// Determine operation mode
		if (firstValue instanceof Boolean) {
			// The first value was boolean. Assume that the rest are too
			if (!((Boolean) firstValue).booleanValue())
				return Boolean.FALSE;

			for (int idx = 1; idx < operands.length; ++idx) {
				if (operands[idx].evaluate(context) != Boolean.TRUE)
					return Boolean.FALSE;
			}
			return Boolean.TRUE;
		}

		// Not a boolean. Assume that we can use an iterator on all values
		Set<?> resultSet = asSet(firstValue, false); // Safe since it will not be modified
		for (int idx = 1; idx < operands.length && !resultSet.isEmpty(); ++idx) {
			Iterator<?> itor = operands[idx].evaluateAsIterator(context);
			Set<Object> retained = new HashSet<Object>();
			while (itor.hasNext()) {
				Object value = itor.next();
				if (resultSet.contains(value))
					retained.add(value);
			}
			resultSet = retained;
		}
		return resultSet;
	}

	public int getExpressionType() {
		return TYPE_AND;
	}

	String getOperator() {
		return OPERATOR_AND;
	}

	int getPriority() {
		return PRIORITY_AND;
	}
}
