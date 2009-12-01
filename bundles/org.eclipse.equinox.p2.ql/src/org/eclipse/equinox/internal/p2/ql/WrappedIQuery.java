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

import java.util.Iterator;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IMatchQuery;
import org.eclipse.equinox.p2.metadata.query.IQuery;

public final class WrappedIQuery extends Constructor {

	static final String KEYWORD = "iquery"; //$NON-NLS-1$

	public WrappedIQuery(Expression[] operands) {
		super(operands);
		int argCount = operands.length;
		if (argCount < 1)
			throw new IllegalArgumentException("iquery must have at least one argument"); //$NON-NLS-1$
		if (argCount > 3)
			throw new IllegalArgumentException("iquery must have max 3 arguments"); //$NON-NLS-1$
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		Object query = operands[0].evaluate(context, scope);

		if (query instanceof IMatchQuery) {
			if (operands.length > 2)
				throw new IllegalArgumentException("iquery third argument cannot be combined with a match query"); //$NON-NLS-1$

			Object value = null;
			if (operands.length > 1)
				value = operands[1].evaluate(context, scope);
			else
				value = Variable.ITEM.evaluate(context, scope);
			return Boolean.valueOf(((IMatchQuery) query).isMatch(value));
		}

		if (!(query instanceof IQuery))
			throw new IllegalArgumentException("iquery first argument must be an IQuery instance"); //$NON-NLS-1$

		Collector collector;
		if (operands.length < 3)
			collector = new Collector();
		else {
			Object cobj = operands[2].evaluate(context, scope);
			if (cobj instanceof Collector)
				collector = (Collector) cobj;
			else if (cobj == null)
				collector = new Collector();
			else
				throw new IllegalArgumentException("iquery third argument must be a collector"); //$NON-NLS-1$
		}

		Iterator iterator = null;
		if (operands.length > 1)
			iterator = operands[1].evaluateAsIterator(context, scope);
		else
			iterator = Variable.EVERYTHING.evaluateAsIterator(context, scope);
		return ((IQuery) query).perform(iterator, collector);
	}

	String getOperator() {
		return KEYWORD;
	}
}
