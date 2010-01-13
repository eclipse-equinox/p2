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
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.expression.Member.DynamicMember;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 * This class represents indexed or keyed access to an indexed collection
 * or a map.
 */
public class At extends Binary {
	protected At(Expression lhs, Expression rhs) {
		super(lhs, rhs);
	}

	protected Object handleMember(IEvaluationContext context, Member member, Object instance, boolean[] handled) {
		if (instance instanceof IInstallableUnit) {
			if ("properties".equals(member.getName())) { //$NON-NLS-1$
				// Avoid full copy of the properties map just to get one member
				handled[0] = true;
				return ((IInstallableUnit) instance).getProperty((String) rhs.evaluate(context));
			}
		}
		return null;
	}

	public Object evaluate(org.eclipse.equinox.p2.metadata.expression.IEvaluationContext context) {
		Object lval;
		if (lhs instanceof DynamicMember) {
			DynamicMember lm = (DynamicMember) lhs;
			Object instance = lm.operand.evaluate(context);
			boolean[] handled = new boolean[] {false};
			Object result = handleMember(context, lm, instance, handled);
			if (handled[0])
				return result;
			lval = lm.invoke(instance);
		} else
			lval = lhs.evaluate(context);

		Object rval = rhs.evaluate(context);
		if (lval == null)
			throw new IllegalArgumentException("Unable to use [] on null"); //$NON-NLS-1$

		if (lval instanceof Map<?, ?>)
			return ((Map<?, ?>) lval).get(rval);

		if (rval instanceof Number) {
			if (lval instanceof List<?>)
				return ((List<?>) lval).get(((Number) rval).intValue());
			if (lval != null && lval.getClass().isArray())
				return ((Object[]) lval)[((Number) rval).intValue()];
		}

		if (lval instanceof Dictionary<?, ?>)
			return ((Dictionary<?, ?>) lval).get(rval);

		throw new IllegalArgumentException("Unable to use [] on a " + lval.getClass().getName()); //$NON-NLS-1$
	}

	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		Object value = evaluate(context);
		if (!(value instanceof Iterator<?>))
			value = RepeatableIterator.create(value);
		return (Iterator<?>) value;
	}

	public int getExpressionType() {
		return TYPE_AT;
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		appendOperand(bld, rootVariable, lhs, getPriority());
		bld.append('[');
		appendOperand(bld, rootVariable, rhs, PRIORITY_MAX);
		bld.append(']');
	}

	public String getOperator() {
		return OPERATOR_AT;
	}

	public int getPriority() {
		return PRIORITY_MEMBER;
	}
}
