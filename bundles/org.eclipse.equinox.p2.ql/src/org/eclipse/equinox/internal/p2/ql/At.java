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

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import java.util.*;

/**
 * This class represents indexed or keyed access to an indexed collection
 * or a map.
 */
public final class At extends Binary {
	public static final String OPERATOR = "[]"; //$NON-NLS-1$

	public At(Expression lhs, Expression rhs) {
		super(lhs, rhs);
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		Object lval;
		if (lhs instanceof Member) {
			Member lm = (Member) lhs;
			Object instance = lm.operand.evaluate(context, scope);
			if (instance instanceof IInstallableUnit && "properties".equals(lm.name)) //$NON-NLS-1$
				// Avoid full copy of the properties map just to get one member
				return ((IInstallableUnit) instance).getProperty((String) rhs.evaluate(context, scope));
			lval = lm.invoke(instance, Member.NO_ARGS);
		} else
			lval = lhs.evaluate(context, scope);

		Object rval = rhs.evaluate(context, scope);
		if (lval == null)
			throw new IllegalArgumentException("Unable to use [] on null"); //$NON-NLS-1$

		if (lval instanceof Map)
			return ((Map) lval).get(rval);

		if (rval instanceof Number) {
			if (lval instanceof List)
				return ((List) lval).get(((Number) rval).intValue());
			if (lval != null && lval.getClass().isArray())
				return ((Object[]) lval)[((Number) rval).intValue()];
		}

		if (lval instanceof Dictionary)
			return ((Dictionary) lval).get(rval);

		throw new IllegalArgumentException("Unable to use [] on a " + lval.getClass().getName()); //$NON-NLS-1$
	}

	public void toString(StringBuffer bld) {
		appendOperand(bld, lhs, getPriority());
		bld.append('[');
		appendOperand(bld, rhs, ExpressionParser.PRIORITY_COMMA);
		bld.append(']');
	}

	String getOperator() {
		return OPERATOR;
	}

	int getPriority() {
		return ExpressionParser.PRIORITY_MEMBER;
	}
}
