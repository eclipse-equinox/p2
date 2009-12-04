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

final class MultiVariableScope implements VariableScope {
	private final VariableScope parentScope;

	private final Object[] values;

	public MultiVariableScope(VariableScope parentScope, Variable[] variables) {
		this.parentScope = parentScope;
		values = new Object[variables.length * 2];
		for (int idx = 0, ndx = 0; ndx < variables.length; ++ndx, idx += 2)
			values[idx] = variables[ndx];
	}

	public Object getValue(Variable variable) {
		for (int idx = 0; idx < values.length; ++idx)
			if (values[idx++] == variable)
				return values[idx];
		return parentScope.getValue(variable);
	}

	public void setValue(Variable variable, Object value) {
		for (int idx = 0; idx < values.length; ++idx)
			if (values[idx++] == variable) {
				values[idx] = value;
				return;
			}
		parentScope.setValue(variable, value);
	}
}