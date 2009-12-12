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
package org.eclipse.equinox.internal.p2.ql.expression;

/**
 * A function that obtains a class based on a String
 */
final class ClassFunction extends Function {

	public ClassFunction(Expression[] operands) {
		super(assertLength(operands, 1, 1, KEYWORD_CLASS));
		assertNotBoolean(operands[0], "parameter"); //$NON-NLS-1$
		assertNotCollection(operands[0], "parameter"); //$NON-NLS-1$
	}

	boolean assertSingleArgumentClass(Object v) {
		return v instanceof String;
	}

	Object createInstance(Object arg) {
		try {
			return Class.forName((String) arg);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	String getOperator() {
		return KEYWORD_CLASS;
	}
}
