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

abstract class UnaryCollectionFilter extends Unary {

	UnaryCollectionFilter(Expression collection) {
		super(collection);
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
		return ExpressionParser.PRIORITY_COLLECTION;
	}
}
