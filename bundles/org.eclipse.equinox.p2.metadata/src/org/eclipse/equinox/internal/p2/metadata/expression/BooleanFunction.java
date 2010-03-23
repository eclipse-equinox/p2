/*******************************************************************************
 * Copyright (c) 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	boolean assertSingleArgumentClass(Object v) {
		return v instanceof String || v instanceof Boolean;
	}

	Object createInstance(Object arg) {
		if (arg instanceof String)
			return Boolean.valueOf("true".equalsIgnoreCase((String) arg)); //$NON-NLS-1$
		if (arg instanceof Boolean)
			return arg;
		return Boolean.FALSE;
	}

	public String getOperator() {
		return KEYWORD_BOOLEAN;
	}
}
