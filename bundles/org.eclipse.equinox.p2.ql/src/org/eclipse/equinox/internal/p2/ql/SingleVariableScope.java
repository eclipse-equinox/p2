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

final class SingleVariableScope implements VariableScope {
	private final VariableScope parentScope;

	private Object value;

	private final Variable variable;

	public SingleVariableScope(Variable variable) {
		this.parentScope = ROOT;
		this.variable = variable;
	}

	public SingleVariableScope(VariableScope parentScope, Variable variable) {
		this.parentScope = parentScope;
		this.variable = variable;
	}

	public Object getValue(Variable var) {
		return variable == var ? value : parentScope.getValue(var);
	}

	public void setValue(Variable var, Object val) {
		if (variable == var)
			value = val;
		else
			parentScope.setValue(var, val);
	}
}