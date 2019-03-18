/*******************************************************************************
 * Copyright (c) 2009 - 2010 Cloudsmith Inc. and others.
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
package org.eclipse.equinox.p2.metadata.expression;

/**
 * A parser that produces an expression tree based on a string representation. An
 * implementation will use the {@link IExpressionFactory} to create the actual expressions
 * @since 2.0
 */
public interface IExpressionParser {
	/**
	 * Create a new expression. The expression will have access to the global
	 * variable 'this' and to the context parameters.
	 * @param exprString The string representing the boolean expression.
	 * @return The resulting expression tree.
	 * @throws ExpressionParseException If an error occurred during parsing.
	 */
	IExpression parse(String exprString);

	/**
	 * Create an arbitrary expression. The expression will have access to the global
	 * variable 'everything' and to the context parameters.
	 * @param exprString The string representing the boolean expression.
	 * @return The resulting expression tree.
	 * @throws ExpressionParseException If an error occurred during parsing.
	 */
	IExpression parseQuery(String exprString);
}
