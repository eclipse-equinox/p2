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
 * A collection filter that yields true if the <code>filter</code> yields true for
 * all of the elements of the <code>collection</code>
 */
public final class All extends CollectionFilter {
	static final String OPERATOR = "all"; //$NON-NLS-1$

	public All(Expression collection, LambdaExpression lambda) {
		super(collection, lambda);
	}

	Object evaluate(ExpressionContext context, VariableScope scope, Iterator itor) {
		while (itor.hasNext()) {
			variable.setValue(scope, itor.next());
			if (lambda.evaluate(context, scope) != Boolean.TRUE)
				return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	String getOperator() {
		return OPERATOR;
	}
}
