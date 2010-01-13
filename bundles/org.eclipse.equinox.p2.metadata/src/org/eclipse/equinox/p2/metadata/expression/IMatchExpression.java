/*******************************************************************************
 * Copyright (c) 2009 - 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.expression;

/**
 * A match expression is a boolean expression matching a candidate of a
 * specific type. An {@link IEvaluationContext} is needed in order to evaluate
 * a match and this class provides two ways of doing that. Either a context
 * is created first and then reused in several subsequent calls to
 * {@link #isMatch(IEvaluationContext, Object)} or, if no repeated calls are
 * expected, the {@link #isMatch(Object)} method can be used. It will then
 * create a context on each call.
 * @since 2.0
 */
public interface IMatchExpression<T> extends IExpression {
	/**
	 * <p>Creates a new context to be passed to repeated subsequent evaluations. The context
	 * will introduce 'this' as an uninitialized variable and make the parameters available.
	 * @return A new evaluation context.
	 */
	IEvaluationContext createContext();

	/**
	 * Returns the parameters that this match expression was created with.
	 * @return An array of parameters, possibly empty but never <code>null</code>.
	 */
	Object[] getParameters();

	/**
	 * This method creates a new evaluation context and assigns the <code>candidate</code>
	 * to the 'this' variable of the <code>context</code> and then evaluates the expression.
	 * This is essentially a short form for <pre>isMatch(createContext(), candidate)</pre>.
	 * @param candidate The object to test.
	 * @return the result of the evaluation.
	 */
	boolean isMatch(T candidate);

	/**
	 * This method assigns <code>candidate</code> to the 'this' variable of the
	 * <code>context</code> and then evaluates the expression.
	 * @param context A context
	 * @param candidate The object to test.
	 * @return the result of the evaluation.
	 */
	boolean isMatch(IEvaluationContext context, T candidate);
}
