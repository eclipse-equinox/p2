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
package org.eclipse.equinox.p2.ql;

/**
 * The item expression is the top expression in item queries. It introduces the
 * variable 'item' and initializes it with the item to match.
 */
public interface IMatchExpression extends IExpression {
	/**
	 * <p>Creates a new context to be passed to a subsequent evaluation. The context
	 * will introduce 'item' as an uninitialized variable and make the parameters available.
	 * @param params The parameters to use for the evaluation.
	 * @return A new evaluation context.
	 */
	IEvaluationContext createContext(Object[] params);

	/**
	 * <p>Creates a new context to be passed to a subsequent evaluation. The context
	 * will introduce 'item' as an uninitialized variable and make the parameters available.
	 * @param params The parameters to use for the evaluation.
	 * @param translations A translation support object to be assigned to the variable 'translations'
	 * @return A new evaluation context.
	 */
	IEvaluationContext createContext(Object[] params, ITranslationSupport translations);

	/**
	 * This method assigns <code>candidate</code> to the 'item' variable of the
	 * <code>context</code> and then evaluates the expression.
	 * @param context A context previously created with {@link #createContext(Object[])}
	 * @param candidate The object to test.
	 * @return the result of the evaluation.
	 */
	boolean isMatch(IEvaluationContext context, Object candidate);
}
