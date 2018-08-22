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
 * A collection filter that yields the first element of the <code>collection</code> for which
 * the <code>filter</code> yields <code>true</code>
 */
final class First extends CollectionFilter {
	First(Expression collection, LambdaExpression lambda) {
		super(collection, lambda);
	}

	@Override
	protected Object evaluate(IEvaluationContext context, Iterator<?> itor) {
		Variable variable = lambda.getItemVariable();
		while (itor.hasNext()) {
			Object each = itor.next();
			variable.setValue(context, each);
			if (lambda.evaluate(context) == Boolean.TRUE)
				return each;
		}
		return null;
	}

	@Override
	public int getExpressionType() {
		return TYPE_FIRST;
	}

	@Override
	public String getOperator() {
		return KEYWORD_FIRST;
	}
}
