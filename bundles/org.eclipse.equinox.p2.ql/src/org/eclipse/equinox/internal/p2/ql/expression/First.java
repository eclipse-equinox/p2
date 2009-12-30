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

import java.util.Iterator;
import org.eclipse.equinox.p2.ql.IEvaluationContext;

/**
 * A collection filter that yields the first element of the <code>collection</code> for which
 * the <code>filter</code> yields <code>true</code>
 */
final class First extends CollectionFilter {
	First(Expression collection, LambdaExpression lambda) {
		super(collection, lambda);
	}

	public int getExpressionType() {
		return TYPE_FIRST;
	}

	Object evaluate(IEvaluationContext context, Iterator<?> itor) {
		Variable variable = lambda.getItemVariable();
		while (itor.hasNext()) {
			Object each = itor.next();
			variable.setValue(context, each);
			if (lambda.evaluate(context) == Boolean.TRUE)
				return each;
		}
		return null;
	}

	String getOperator() {
		return KEYWORD_FIRST;
	}

	boolean isBoolean() {
		return operand.isElementBoolean();
	}

	boolean isCollection() {
		return operand.isElementCollection();
	}
}
