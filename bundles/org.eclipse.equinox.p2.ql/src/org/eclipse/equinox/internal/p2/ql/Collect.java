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
 * <code>collection</code> for which the <code>filter</code> yields <code>true</code>.
 */
public final class Collect extends CollectionFilter {
	public static final String OPERATOR = "collect"; //$NON-NLS-1$

	public Collect(Expression collection, LambdaExpression lambda) {
		super(collection, lambda);
	}

	Object evaluate(ExpressionContext context, VariableScope scope, Iterator itor) {
		return new CollectIterator(context, scope, variable, itor, lambda);
	}

	String getOperator() {
		return OPERATOR;
	}
}
