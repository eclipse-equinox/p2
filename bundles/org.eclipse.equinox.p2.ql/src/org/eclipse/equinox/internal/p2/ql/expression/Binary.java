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

import org.eclipse.equinox.p2.ql.IExpressionVisitor;

/**
 * The abstract base class for all binary operations
 */
abstract class Binary extends Expression {
	final Expression lhs;

	final Expression rhs;

	Binary(Expression lhs, Expression rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public boolean accept(IExpressionVisitor visitor) {
		return super.accept(visitor) && lhs.accept(visitor) && rhs.accept(visitor);
	}

	public void toString(StringBuffer bld) {
		appendOperand(bld, lhs, getPriority());
		bld.append(' ');
		bld.append(getOperator());
		bld.append(' ');
		appendOperand(bld, rhs, getPriority());
	}

	int countReferenceToEverything() {
		return lhs.countReferenceToEverything() + rhs.countReferenceToEverything();
	}

	int getPriority() {
		return PRIORITY_BINARY; // Default priority
	}
}
