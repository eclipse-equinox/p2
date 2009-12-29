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

import java.util.*;
import org.eclipse.equinox.internal.p2.ql.IRepeatableIterator;
import org.eclipse.equinox.internal.p2.ql.RepeatableIterator;
import org.eclipse.equinox.internal.p2.ql.parser.IParserConstants;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;
import org.eclipse.equinox.p2.ql.*;

/**
 * The base class of the expression tree.
 */
abstract class Expression implements IExpression, IParserConstants {

	static final Expression[] emptyArray = new Expression[0];

	static Set<?> asSet(Object val, boolean forcePrivateCopy) {
		if (val == null)
			throw new IllegalArgumentException("Cannot convert null into an set"); //$NON-NLS-1$

		if (val instanceof IRepeatableIterator<?>) {
			Object provider = ((IRepeatableIterator<?>) val).getIteratorProvider();
			if (!forcePrivateCopy) {
				if (provider instanceof Set<?>)
					return (Set<?>) provider;
				if (provider instanceof IQueryResult<?>)
					return ((IQueryResult<?>) provider).unmodifiableSet();
			}

			if (provider instanceof Collection<?>)
				val = provider;
		} else {
			if (!forcePrivateCopy) {
				if (val instanceof Set<?>)
					return (Set<?>) val;
				if (val instanceof IQueryResult<?>)
					return ((IQueryResult<?>) val).unmodifiableSet();
			}
		}

		HashSet<Object> result;
		if (val instanceof Collection<?>)
			result = new HashSet<Object>((Collection<?>) val);
		else {
			result = new HashSet<Object>();
			Iterator<?> iterator = RepeatableIterator.create(val);
			while (iterator.hasNext())
				result.add(iterator.next());
		}
		return result;
	}

	/**
	 * Let the visitor visit this instance and all expressions that this
	 * instance contains.
	 * @param visitor The visiting visitor.
	 * @return <code>true</code> if the visitor should continue visiting, <code>false</code> otherwise.
	 */
	public boolean accept(IExpressionVisitor visitor) {
		return visitor.accept(this);
	}

	/**
	 * Evaluate this expression with given context and variables.
	 * @param context The evaluation context
	 * @return The result of the evaluation.
	 */
	public abstract Object evaluate(IEvaluationContext context);

	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		Object value = evaluate(context);
		if (!(value instanceof Iterator<?>))
			value = RepeatableIterator.create(value);
		return (Iterator<?>) value;
	}

	/**
	 * Checks if the expression will make repeated requests for the 'everything' iterator.
	 * @return <code>true</code> if repeated requests will be made, <code>false</code> if not.
	 */
	public boolean needsRepeatedIterations() {
		return countReferenceToEverything() > 1;
	}

	public final boolean needsTranslations() {
		final boolean[] translationSupportNeeded = new boolean[] {false};
		accept(new IExpressionVisitor() {
			public boolean accept(IExpression expr) {
				if (((Expression) expr).isReferencingTranslations()) {
					translationSupportNeeded[0] = true;
					return false;
				}
				return true;
			}
		});
		return translationSupportNeeded[0];
	}

	public String toString() {
		StringBuffer bld = new StringBuffer();
		toString(bld);
		return bld.toString();
	}

	public abstract void toString(StringBuffer bld);

	static void appendOperand(StringBuffer bld, Expression operand, int priority) {
		if (priority < operand.getPriority()) {
			bld.append('(');
			operand.toString(bld);
			bld.append(')');
		} else
			operand.toString(bld);
	}

	void assertNotCollection(Expression expr, String usage) {
		if (expr.isCollection())
			throw new IllegalArgumentException("A collection cannot be used as " + usage + " in a " + getOperator()); //$NON-NLS-1$//$NON-NLS-2$
	}

	void assertNotBoolean(Expression expr, String usage) {
		if (expr.isBoolean())
			throw new IllegalArgumentException("A boolean cannot be used as " + usage + " in a " + getOperator()); //$NON-NLS-1$//$NON-NLS-2$
	}

	int countReferenceToEverything() {
		return 0;
	}

	abstract String getOperator();

	abstract int getPriority();

	boolean isBoolean() {
		return false;
	}

	boolean isCollection() {
		return false;
	}

	boolean isElementBoolean() {
		return false;
	}

	boolean isReferencingTranslations() {
		return false;
	}

	boolean isPipeable() {
		// TODO Auto-generated method stub
		return false;
	}

	Expression pipeFrom(Expression nxt) {
		// TODO Auto-generated method stub
		return null;
	}

	boolean isElementCollection() {
		// TODO Auto-generated method stub
		return false;
	}
}
