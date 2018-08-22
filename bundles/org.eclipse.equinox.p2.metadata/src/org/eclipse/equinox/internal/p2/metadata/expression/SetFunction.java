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

import java.util.HashSet;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

public final class SetFunction extends Function {

	public SetFunction(Expression[] operands) {
		super(operands);
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		HashSet<Object> result = new HashSet<>();
		for (int idx = 0; idx < operands.length; ++idx)
			result.add(operands[idx].evaluate(context));
		return result;
	}

	@Override
	public String getOperator() {
		return KEYWORD_SET;
	}

	boolean isCollection() {
		return true;
	}
}
