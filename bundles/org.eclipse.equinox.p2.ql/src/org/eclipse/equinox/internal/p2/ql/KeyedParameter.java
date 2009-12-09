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

/**
 * An expression that picks an keyed parameter from the expression context.
 */
public final class KeyedParameter extends Parameter {
	final String key;

	public KeyedParameter(String key) {
		this.key = key;
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		return context.getParameter(key);
	}

	public void toString(StringBuffer bld) {
		bld.append('$');
		bld.append(key);
	}
}
