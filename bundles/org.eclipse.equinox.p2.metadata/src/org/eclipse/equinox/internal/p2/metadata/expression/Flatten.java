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
 * An expression that yields a new collection consisting of all elements of the
 * <code>collection</code> for which the <code>filter</code> yields <code>true</code>.
 */
final class Flatten extends UnaryCollectionFilter {
	Flatten(Expression collection) {
		super(collection);
	}

	@Override
	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		return new CompoundIterator<>(operand.evaluateAsIterator(context));
	}

	@Override
	public int getExpressionType() {
		return TYPE_FLATTEN;
	}

	@Override
	public String getOperator() {
		return IExpressionConstants.KEYWORD_FLATTEN;
	}
}
