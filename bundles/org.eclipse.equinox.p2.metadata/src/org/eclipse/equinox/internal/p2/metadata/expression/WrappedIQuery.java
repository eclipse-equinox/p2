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

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.query.IMatchQuery;
import org.eclipse.equinox.p2.query.IQuery;

public final class WrappedIQuery extends Function {

	/**
	 * <p>The WrappedIQuery constructor takes an array with one or two arguments.
	 * The first argument must evaluate to an instance of {@link IQuery}. The second
	 * argument is optional. The following applies:</p><ul>
	 * <li>If first argument evaluates to an instance of {@link IMatchQuery}, then
	 * a provided second argument assumed to be the candidate to match. The
	 * variable <code>this</code> will be used if no second argument is not provided.</li>
	 * <li>For all other types of queries the second argument must evaluate
	 * to an iterator. If it is not provided, it defaults to the variable
	 * <code>everything</code>.
	 * </ul>
	 * @param operands
	 */
	public WrappedIQuery(Expression[] operands) {
		super(assertLength(operands, 1, 2, KEYWORD_IQUERY));
	}

	@SuppressWarnings("unchecked")
	public Object evaluate(IEvaluationContext context) {
		Object query = operands[0].evaluate(context);

		if (query instanceof IMatchQuery<?>) {
			Object value = null;
			if (operands.length > 1)
				value = operands[1].evaluate(context);
			else
				value = ExpressionFactory.THIS.evaluate(context);
			return Boolean.valueOf(((IMatchQuery<Object>) query).isMatch(value));
		}

		if (!(query instanceof IQuery<?>))
			throw new IllegalArgumentException("iquery first argument must be an IQuery instance"); //$NON-NLS-1$

		Iterator<?> iterator = null;
		if (operands.length > 1)
			iterator = operands[1].evaluateAsIterator(context);
		else
			iterator = ExpressionFactory.EVERYTHING.evaluateAsIterator(context);

		return ((IQuery<Object>) query).perform((Iterator<Object>) iterator);
	}

	public String getOperator() {
		return KEYWORD_IQUERY;
	}

	@Override
	public boolean isReferenceTo(Variable variable) {
		Object firstOp = operands[0];
		if (firstOp instanceof Literal) {
			Object query = ((Literal) firstOp).value;
			return (query instanceof IMatchQuery<?>) ? variable == ExpressionFactory.THIS : variable == ExpressionFactory.EVERYTHING;
		}
		return false;
	}

	int countAccessToEverything() {
		return isReferenceTo(ExpressionFactory.EVERYTHING) ? 1 : 0;
	}
}
