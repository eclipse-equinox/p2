/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;


import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;

/**
 * A function that creates an OSGi filter based on a String
 */
public final class FilterFunction extends Function {
	public FilterFunction(Expression[] operands) {
		super(assertLength(operands, 1, 1, KEYWORD_FILTER));
	}

	boolean assertSingleArgumentClass(Object v) {
		return v instanceof String;
	}

	Object createInstance(Object arg) {
		return ExpressionUtil.parseLDAP((String) arg);
	}

	public String getOperator() {
		return KEYWORD_FILTER;
	}
}
