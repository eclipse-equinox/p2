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

import java.util.Iterator;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IMatchQuery;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.ql.IEvaluationContext;
import org.eclipse.equinox.p2.ql.QLContextQuery;

final class WrappedIQuery extends Function {

	public WrappedIQuery(Expression[] operands) {
		super(assertLength(operands, 1, 3, KEYWORD_IQUERY));
		assertNotBoolean(operands[0], "parameter"); //$NON-NLS-1$
		assertNotCollection(operands[0], "parameter"); //$NON-NLS-1$
	}

	public Object evaluate(IEvaluationContext context) {
		Object query = operands[0].evaluate(context);

		if (query instanceof IMatchQuery) {
			if (operands.length > 2)
				throw new IllegalArgumentException("iquery third argument cannot be combined with a match query"); //$NON-NLS-1$

			Object value = null;
			if (operands.length > 1)
				value = operands[1].evaluate(context);
			else
				value = Variable.ITEM.evaluate(context);
			return Boolean.valueOf(((IMatchQuery) query).isMatch(value));
		}

		if (!(query instanceof IQuery))
			throw new IllegalArgumentException("iquery first argument must be an IQuery instance"); //$NON-NLS-1$

		Collector collector = null;
		if (operands.length == 3) {
			Object cobj = operands[2].evaluate(context);
			if (cobj instanceof Collector)
				collector = (Collector) cobj;
			else if (cobj == null)
				collector = new Collector();
			else
				throw new IllegalArgumentException("iquery third argument must be a collector"); //$NON-NLS-1$
		}

		Iterator iterator = null;
		if (operands.length > 1)
			iterator = operands[1].evaluateAsIterator(context);
		else
			iterator = Variable.EVERYTHING.evaluateAsIterator(context);

		if (collector == null) {
			if (query instanceof QLContextQuery)
				return ((QLContextQuery) query).evaluate(iterator);
			collector = new Collector();
		}
		return ((IQuery) query).perform(iterator, collector);
	}

	String getOperator() {
		return KEYWORD_IQUERY;
	}
}
