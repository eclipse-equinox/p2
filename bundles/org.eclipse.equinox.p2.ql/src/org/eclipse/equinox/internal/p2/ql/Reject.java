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
 * An expression that yields a new collection consisting of all elements of the
 * <code>collection</code> for which the <code>filter</code> does not yield <code>true</code>.
 */
public class Reject extends CollectionFilter {
	public static final String OPERATOR = "reject"; //$NON-NLS-1$

	public Reject(Expression collection, LambdaExpression lambda) {
		super(collection, lambda);
	}

	Object evaluate(final ExpressionContext context, final VariableScope scope, Iterator iterator) {
		return new MatchIteratorFilter(Object.class, iterator) {
			protected boolean isMatch(Object val) {
				scope.setEach(variable, val);
				return lambda.evaluate(context, scope) != Boolean.TRUE;
			}
		};
	}

	protected String getOperator() {
		return OPERATOR;
	}
}
