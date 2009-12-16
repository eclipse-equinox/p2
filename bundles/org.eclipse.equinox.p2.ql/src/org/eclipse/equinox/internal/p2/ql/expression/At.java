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
import org.eclipse.equinox.internal.p2.ql.expression.Member.DynamicMember;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ql.IEvaluationContext;
import org.eclipse.equinox.p2.ql.ITranslationSupport;

/**
 * This class represents indexed or keyed access to an indexed collection
 * or a map.
 */
final class At extends Binary {
	At(Expression lhs, Expression rhs) {
		super(lhs, rhs);
	}

	public Object evaluate(IEvaluationContext context) {
		Object lval;
		if (lhs instanceof DynamicMember) {
			DynamicMember lm = (DynamicMember) lhs;
			Object instance = lm.operand.evaluate(context);
			if (instance instanceof IInstallableUnit) {
				if ("properties".equals(lm.name)) //$NON-NLS-1$
					// Avoid full copy of the properties map just to get one member
					return ((IInstallableUnit) instance).getProperty((String) rhs.evaluate(context));

				if (VARIABLE_TRANSLATIONS.equals(lm.name)) {
					ITranslationSupport ts = (ITranslationSupport) Variable.TRANSLATIONS.evaluate(context);
					return ts.getIUProperty((IInstallableUnit) instance, (String) rhs.evaluate(context));
				}
			}
			lval = lm.invoke(instance);
		} else
			lval = lhs.evaluate(context);

		Object rval = rhs.evaluate(context);
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

	public int getExpressionType() {
		return TYPE_AT;
	}

	public void toString(StringBuffer bld) {
		appendOperand(bld, lhs, getPriority());
		bld.append('[');
		appendOperand(bld, rhs, PRIORITY_COMMA);
		bld.append(']');
	}

	String getOperator() {
		return OPERATOR_AT;
	}

	int getPriority() {
		return PRIORITY_MEMBER;
	}
}
