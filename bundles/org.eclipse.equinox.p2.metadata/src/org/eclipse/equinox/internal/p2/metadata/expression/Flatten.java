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
 * An expression that yields a new collection consisting of all elements of the
 * <code>collection</code> for which the <code>filter</code> yields <code>true</code>.
 */
final class Flatten extends UnaryCollectionFilter {
	Flatten(Expression collection) {
		super(collection);
	}

	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		return new CompoundIterator<Object>(operand.evaluateAsIterator(context));
	}

	public int getExpressionType() {
		return TYPE_FLATTEN;
	}

	public String getOperator() {
		return IExpressionConstants.KEYWORD_FLATTEN;
	}
}
