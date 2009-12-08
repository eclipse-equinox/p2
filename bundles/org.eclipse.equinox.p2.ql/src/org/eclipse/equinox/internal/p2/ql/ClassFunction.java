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
 * A function that obtains a class based on a String
 */
public class ClassFunction extends Function {

	static final String KEYWORD = "class"; //$NON-NLS-1$

	public ClassFunction(Expression[] operands) {
		super(operands);
	}

	Object createInstance(String arg) {
		try {
			return Class.forName(arg);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	String getOperator() {
		return KEYWORD;
	}
}
