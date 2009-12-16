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

import java.util.Map;
import org.eclipse.equinox.p2.ql.IEvaluationContext;
import org.eclipse.equinox.p2.ql.IExpression;

/**
 * The immutable top level context used when evaluating an expression.
 */
public final class ParameterContext implements IEvaluationContext {
	private static final Object[] noParameters = new Object[0];

	private final Object[] parameters;

	public ParameterContext(Object[] parameters) {
		this.parameters = parameters == null ? noParameters : parameters;
	}

	public Object getParameter(int position) {
		return parameters[position];
	}

	public Object getParameter(String key) {
		return parameters.length == 1 && parameters[0] instanceof Map ? ((Map) parameters[0]).get(key) : null;
	}

	public Object getValue(IExpression variable) {
		throw new IllegalArgumentException("No such variable: " + variable); //$NON-NLS-1$
	}

	public void setValue(IExpression variable, Object value) {
		throw new IllegalArgumentException("No such variable: " + variable); //$NON-NLS-1$
	}
}
