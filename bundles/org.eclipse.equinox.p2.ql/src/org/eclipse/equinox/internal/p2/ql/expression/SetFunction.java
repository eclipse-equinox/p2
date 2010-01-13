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

import java.util.HashSet;
import org.eclipse.equinox.internal.p2.metadata.expression.Expression;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

public final class SetFunction extends Function implements IQLConstants {

	public SetFunction(Expression[] operands) {
		super(operands);
	}

	public Object evaluate(IEvaluationContext context) {
		HashSet<Object> result = new HashSet<Object>();
		for (int idx = 0; idx < operands.length; ++idx)
			result.add(operands[idx].evaluate(context));
		return result;
	}

	public String getOperator() {
		return KEYWORD_SET;
	}

	boolean isCollection() {
		return true;
	}
}
