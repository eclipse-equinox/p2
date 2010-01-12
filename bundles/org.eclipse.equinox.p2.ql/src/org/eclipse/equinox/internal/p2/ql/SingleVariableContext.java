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

import org.eclipse.equinox.p2.ql.IEvaluationContext;
import org.eclipse.equinox.p2.ql.IExpression;

public final class SingleVariableContext implements IEvaluationContext {
	private final IEvaluationContext parentContext;

	private Object value;

	private final IExpression variable;

	public SingleVariableContext(IEvaluationContext parentContext, IExpression variable) {
		this.parentContext = parentContext;
		this.variable = variable;
	}

	public Object getParameter(int position) {
		return parentContext.getParameter(position);
	}

	public Object getParameter(String key) {
		return parentContext.getParameter(key);
	}

	public Object getValue(IExpression var) {
		return variable == var ? value : parentContext.getValue(var);
	}

	public void setValue(IExpression var, Object val) {
		if (variable == var)
			value = val;
		else
			parentContext.setValue(var, val);
	}
}