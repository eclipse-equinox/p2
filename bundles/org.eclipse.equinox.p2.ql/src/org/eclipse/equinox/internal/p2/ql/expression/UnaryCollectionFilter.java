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

import org.eclipse.equinox.p2.ql.IEvaluationContext;

abstract class UnaryCollectionFilter extends Unary {

	UnaryCollectionFilter(Expression collection) {
		super(collection);
	}

	public Object evaluate(IEvaluationContext context) {
		return evaluateAsIterator(context);
	}

	public void toString(StringBuffer bld) {
		if (operand instanceof Select) {
			Select select = (Select) operand;
			CollectionFilter.appendProlog(bld, select.operand, getOperator());
			appendOperand(bld, select.lambda, getPriority());
		} else
			CollectionFilter.appendProlog(bld, operand, getOperator());
		bld.append(')');
	}

	int getPriority() {
		return PRIORITY_COLLECTION;
	}

	boolean isCollection() {
		return true;
	}

	boolean isElementBoolean() {
		return operand.isElementBoolean();
	}
}
