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

/**
 * The abstract base class for the indexed and keyed parameters
 */
abstract class Parameter extends Expression {
	public int getExpressionType() {
		return TYPE_PARAMETER;
	}

	int getPriority() {
		return PRIORITY_PARAMETER;
	}

	String getOperator() {
		return OPERATOR_PARAMETER;
	}

	static final class Indexed extends Parameter {
		final int position;

		Indexed(int position) {
			this.position = position;
		}

		public Object evaluate(IEvaluationContext context) {
			return context.getParameter(position);
		}

		public void toString(StringBuffer bld) {
			bld.append('$');
			bld.append(position);
		}
	}

	static final class Keyed extends Parameter {
		final String key;

		public Keyed(String key) {
			this.key = key;
		}

		public Object evaluate(IEvaluationContext context) {
			return context.getParameter(key);
		}

		public void toString(StringBuffer bld) {
			bld.append('$');
			bld.append(key);
		}
	}
}
