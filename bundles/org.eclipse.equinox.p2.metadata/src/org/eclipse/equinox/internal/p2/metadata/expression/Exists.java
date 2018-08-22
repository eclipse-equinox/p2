/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 * A collection filter that yields true if the <code>filter</code> yields true for
 * any of the elements of the <code>collection</code>
 */
final class Exists extends CollectionFilter {
	Exists(Expression collection, LambdaExpression lambda) {
		super(collection, lambda);
	}

	@Override
	protected Object evaluate(IEvaluationContext context, Iterator<?> itor) {
		Variable variable = lambda.getItemVariable();
		while (itor.hasNext()) {
			variable.setValue(context, itor.next());
			if (lambda.evaluate(context) == Boolean.TRUE)
				return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	@Override
	public int getExpressionType() {
		return TYPE_EXISTS;
	}

	@Override
	public String getOperator() {
		return KEYWORD_EXISTS;
	}
}
