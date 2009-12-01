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

import org.osgi.framework.InvalidSyntaxException;

public class FilterConstructor extends Constructor {

	static final String KEYWORD = "filter"; //$NON-NLS-1$

	public FilterConstructor(Expression[] operands) {
		super(operands);
	}

	Object createInstance(String arg) {
		try {
			return QLActivator.context.createFilter(arg);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	String getOperator() {
		return KEYWORD;
	}
}
