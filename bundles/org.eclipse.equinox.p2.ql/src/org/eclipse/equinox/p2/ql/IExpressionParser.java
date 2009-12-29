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
 * A parser that produces an expression tree based on a string representation. An
 * implementation will use the {@link IExpressionFactory} to create the actual expressions
 */
public interface IExpressionParser {
	/**
	 * Create a new boolean expression. The expression will have access to the global
	 * variable 'item' and to the context parameters.
	 * @param exprString The string representing the boolean expression.
	 * @return The resulting expression tree.
	 * @throws QLParseException
	 */
	IMatchExpression parsePredicate(String exprString);

	/**
	 * Create an arbitrary expression. The expression will have access to the global
	 * variable 'everything' and to the context parameters.
	 * @param exprString The string representing the boolean expression.
	 * @return The resulting expression tree.
	 * @throws QLParseException
	 */
	<T> IContextExpression<T> parseQuery(Class<T> elementClass, String exprString);
}
