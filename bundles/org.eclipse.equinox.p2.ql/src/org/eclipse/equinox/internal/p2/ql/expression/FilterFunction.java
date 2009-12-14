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

import org.eclipse.equinox.internal.p2.metadata.LDAPQuery;
import org.eclipse.equinox.internal.p2.ql.QLActivator;
import org.osgi.framework.InvalidSyntaxException;

/**
 * A function that creates an OSGi filter based on a String
 */
final class FilterFunction extends Function {
	public FilterFunction(Expression[] operands) {
		super(assertLength(operands, 1, 1, KEYWORD_FILTER));
		assertNotBoolean(operands[0], "parameter"); //$NON-NLS-1$
		assertNotCollection(operands[0], "parameter"); //$NON-NLS-1$
	}

	boolean assertSingleArgumentClass(Object v) {
		return v instanceof LDAPQuery || v instanceof String;
	}

	Object createInstance(Object arg) {
		String str = (arg instanceof LDAPQuery) ? ((LDAPQuery) arg).getFilter() : (String) arg;
		try {
			return QLActivator.context.createFilter(str);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	String getOperator() {
		return KEYWORD_FILTER;
	}
}
