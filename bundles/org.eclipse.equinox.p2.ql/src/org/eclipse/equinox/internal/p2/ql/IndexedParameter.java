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
 * An expression that picks an indexed parameter from the expression context.
 */
public final class IndexedParameter extends Parameter {
	final int position;

	public IndexedParameter(int position) {
		this.position = position;
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		return context.getParameter(position);
	}

	public void toString(StringBuffer bld) {
		bld.append('$');
		bld.append(position);
	}
}
