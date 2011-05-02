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

import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

abstract class UnaryCollectionFilter extends Unary {

	UnaryCollectionFilter(Expression collection) {
		super(collection);
	}

	public int hashCode() {
		return 5 * operand.hashCode();
	}

	public Object evaluate(IEvaluationContext context) {
		return evaluateAsIterator(context);
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		if (operand instanceof Select) {
			Select select = (Select) operand;
			CollectionFilter.appendProlog(bld, rootVariable, select.operand, getOperator());
			appendOperand(bld, rootVariable, select.lambda, getPriority());
		} else
			CollectionFilter.appendProlog(bld, rootVariable, operand, getOperator());
		bld.append(')');
	}

	public int getPriority() {
		return PRIORITY_COLLECTION;
	}
}
