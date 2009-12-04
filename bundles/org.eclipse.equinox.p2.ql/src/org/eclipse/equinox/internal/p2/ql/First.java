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

import java.util.Iterator;

/**
 * A collection filter that yields the first element of the <code>collection</code> for which
 * the <code>filter</code> yields <code>true</code>
 */
public final class First extends CollectionFilter {
	public static final String OPERATOR = "first"; //$NON-NLS-1$

	public First(Expression collection, LambdaExpression lambda) {
		super(collection, lambda);
	}

	Object evaluate(ExpressionContext context, VariableScope scope, Iterator itor) {
		while (itor.hasNext()) {
			Object each = itor.next();
			variable.setValue(scope, each);
			if (lambda.evaluate(context, scope) == Boolean.TRUE)
				return each;
		}
		return null;
	}

	String getOperator() {
		return OPERATOR;
	}
}
