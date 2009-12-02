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

import java.util.HashSet;

public class SetConstructor extends Constructor {

	static final String KEYWORD = "set"; //$NON-NLS-1$

	public SetConstructor(Expression[] operands) {
		super(operands);
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		HashSet result = new HashSet();
		for (int idx = 0; idx < operands.length; ++idx)
			result.add(operands[idx].evaluate(context, scope));
		return result;
	}

	String getOperator() {
		return KEYWORD;
	}
}
