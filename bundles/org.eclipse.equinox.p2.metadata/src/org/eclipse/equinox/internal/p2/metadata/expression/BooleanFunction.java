/*******************************************************************************
 * Copyright (c) 2010, 2017 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

/**
 * A function that obtains a class based on a String
 */
public final class BooleanFunction extends Function {

	public BooleanFunction(Expression[] operands) {
		super(assertLength(operands, 1, 1, KEYWORD_BOOLEAN));
	}

	@Override
	boolean assertSingleArgumentClass(Object v) {
		return v instanceof String || v instanceof Boolean;
	}

	@Override
	Object createInstance(Object arg) {
		if (arg instanceof String)
			return Boolean.valueOf("true".equalsIgnoreCase((String) arg)); //$NON-NLS-1$
		if (arg instanceof Boolean)
			return arg;
		return Boolean.FALSE;
	}

	@Override
	public String getOperator() {
		return KEYWORD_BOOLEAN;
	}
}
