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

import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

abstract class UnaryCollectionFilter extends Unary {

	UnaryCollectionFilter(Expression collection) {
		super(collection);
	}

	@Override
	public int hashCode() {
		return 5 * operand.hashCode();
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		return evaluateAsIterator(context);
	}

	@Override
	public void toString(StringBuffer bld, Variable rootVariable) {
		if (operand instanceof Select) {
			Select select = (Select) operand;
			CollectionFilter.appendProlog(bld, rootVariable, select.operand, getOperator());
			appendOperand(bld, rootVariable, select.lambda, getPriority());
		} else
			CollectionFilter.appendProlog(bld, rootVariable, operand, getOperator());
		bld.append(')');
	}

	@Override
	public int getPriority() {
		return PRIORITY_COLLECTION;
	}
}
