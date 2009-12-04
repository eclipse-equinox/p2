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

import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;

/**
 * The base class of the expression tree.
 */
public abstract class Expression {

	public interface Visitor {
		/**
		 * The method that will be called for each expression that is
		 * visited.
		 * @param expression The expression that the visitor visits.
		 * @return <code>true</code> to continue visiting other expressions or
		 * <code>false</code> to break out.
		 */
		boolean accept(Expression expression);
	}

	public static final Expression[] emptyArray = new Expression[0];

	static Set asSet(Object val, boolean forcePrivateCopy) {
		if (val == null)
			throw new IllegalArgumentException("Cannot convert null into an set"); //$NON-NLS-1$

		if (val instanceof IRepeatableIterator) {
			Object provider = ((IRepeatableIterator) val).getIteratorProvider();
			if (!forcePrivateCopy) {
				if (provider instanceof Set)
					return (Set) provider;
				if (provider instanceof Collector)
					return (Set) ((Collector) provider).toCollection();
			}

			if (provider instanceof Collection)
				val = provider;
		} else {
			if (!forcePrivateCopy) {
				if (val instanceof Set)
					return (Set) val;
				if (val instanceof Collector)
					return (Set) ((Collector) val).toCollection();
			}
		}

		HashSet result;
		if (val instanceof Collection)
			result = new HashSet((Collection) val);
		else {
			result = new HashSet();
			Iterator iterator = RepeatableIterator.create(val);
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
	public boolean accept(Visitor visitor) {
		return visitor.accept(this);
	}

	/**
	 * Evaluate this expression in the given context.
	 * @param scope TODO
	 * @return The result of the evaluation.
	 */
	public abstract Object evaluate(ExpressionContext context, VariableScope scope);

	public Iterator evaluateAsIterator(ExpressionContext context, VariableScope scope) {
		Object value = evaluate(context, scope);
		if (!(value instanceof Iterator))
			value = RepeatableIterator.create(value);
		return (Iterator) value;
	}

	/**
	 * Checks if the expression will make repeated requests for the 'everything' iterator.
	 * @return <code>true</code> if repeated requests will be made, <code>false</code> if not.
	 */
	public boolean needsRepeatedIterations() {
		return countReferenceToEverything() > 1;
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

	int countReferenceToEverything() {
		return 0;
	}

	abstract int getPriority();
}
