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

import java.util.IdentityHashMap;

public final class VariableScope {
	public static final VariableScope DUMMY = new VariableScope();

	private final VariableScope parentScope;

	private IdentityHashMap values;

	private EachVariable eachVariable;
	private Object each;
	private Object item;

	public VariableScope() {
		parentScope = null;
	}

	VariableScope(VariableScope parentScope) {
		this.parentScope = parentScope;
		this.each = parentScope.each;
		this.eachVariable = parentScope.eachVariable;
		this.item = parentScope.item;
	}

	synchronized Object getValue(Variable variable) {
		Object value = null;
		if (values == null)
			return parentScope == null ? null : parentScope.getValue(variable);

		value = values.get(variable);
		if (value == null && parentScope != null && !values.containsKey(variable))
			value = parentScope.getValue(variable);
		return value;
	}

	synchronized void setValue(Variable variable, Object value) {
		if (values == null)
			values = new IdentityHashMap(5);
		values.put(variable, value);
	}

	Object getItem() {
		return item;
	}

	void setItem(Object value) {
		item = value;
	}

	Object getEach(EachVariable eachVar) {
		Object value = each;
		if (eachVar != eachVariable) {
			value = null;
			if (parentScope != null)
				value = parentScope.getEach(eachVar);
		}
		return value;
	}

	void setEach(EachVariable eachVar, Object value) {
		eachVariable = eachVar;
		each = value;
	}
}
