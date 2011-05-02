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

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 * A collection filter that yields true if the <code>filter</code> yields true for
 * all of the elements of the <code>collection</code>
 */
final class All extends CollectionFilter {
	All(Expression collection, LambdaExpression lambda) {
		super(collection, lambda);
	}

	protected Object evaluate(IEvaluationContext context, Iterator<?> itor) {
		Variable variable = lambda.getItemVariable();
		while (itor.hasNext()) {
			variable.setValue(context, itor.next());
			if (lambda.evaluate(context) != Boolean.TRUE)
				return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	public int getExpressionType() {
		return TYPE_ALL;
	}

	public String getOperator() {
		return KEYWORD_ALL;
	}
}
