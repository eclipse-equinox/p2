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

import org.eclipse.equinox.p2.ql.IExpression;
import org.eclipse.equinox.p2.ql.IEvaluationContext;

public final class MultiVariableContext implements IEvaluationContext {
	private final IEvaluationContext parentContext;

	private final Object[] values;

	public MultiVariableContext(IEvaluationContext parentContext, IExpression[] variables) {
		this.parentContext = parentContext;
		values = new Object[variables.length * 2];
		for (int idx = 0, ndx = 0; ndx < variables.length; ++ndx, idx += 2)
			values[idx] = variables[ndx];
	}

	public Object getParameter(int position) {
		return parentContext.getParameter(position);
	}

	public Object getParameter(String key) {
		return parentContext.getParameter(key);
	}

	public Object getValue(IExpression variable) {
		for (int idx = 0; idx < values.length; ++idx)
			if (values[idx++] == variable)
				return values[idx];
		return parentContext.getValue(variable);
	}

	public void setValue(IExpression variable, Object value) {
		for (int idx = 0; idx < values.length; ++idx)
			if (values[idx++] == variable) {
				values[idx] = value;
				return;
			}
		parentContext.setValue(variable, value);
	}
}