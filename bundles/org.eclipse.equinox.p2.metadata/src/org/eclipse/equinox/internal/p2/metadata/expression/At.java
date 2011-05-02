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
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.expression.Member.DynamicMember;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;

/**
 * This class represents indexed or keyed access to an indexed collection
 * or a map.
 */
class At extends Binary {
	protected At(Expression lhs, Expression rhs) {
		super(lhs, rhs);
	}

	public Object evaluate(org.eclipse.equinox.p2.metadata.expression.IEvaluationContext context) {
		Object lval;
		if (lhs instanceof DynamicMember) {
			DynamicMember lm = (DynamicMember) lhs;
			Object instance = lm.operand.evaluate(context);
			if (instance instanceof IInstallableUnit) {
				String name = lm.getName();
				if (InstallableUnit.MEMBER_TRANSLATED_PROPERTIES == name || InstallableUnit.MEMBER_PROFILE_PROPERTIES == name) {
					IIndexProvider<?> indexProvider = context.getIndexProvider();
					if (indexProvider == null)
						throw new UnsupportedOperationException("No managed properties available to QL"); //$NON-NLS-1$
					return indexProvider.getManagedProperty(instance, name, rhs.evaluate(context));
				}
				if (InstallableUnit.MEMBER_PROPERTIES == name) {
					// Avoid full copy of the properties map just to get one member
					return ((IInstallableUnit) instance).getProperty((String) rhs.evaluate(context));
				}
			}
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
