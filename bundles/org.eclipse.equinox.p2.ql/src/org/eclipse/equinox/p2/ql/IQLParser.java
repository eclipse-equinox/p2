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

import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.expression.IExpressionParser;

/**
 * A parser that produces an expression tree based on a string representation. An
 * implementation will use the {@link IQLFactory} to create the actual expressions
 */
public interface IQLParser extends IExpressionParser {
	/**
	 * Create an arbitrary expression. The expression will have access to the global
	 * variable 'everything' and to the context parameters.
	 * @param exprString The string representing the boolean expression.
	 * @return The resulting expression tree.
	 * @throws QLParseException
	 */
	IExpression parseQuery(String exprString);
}
