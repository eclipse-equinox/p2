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
 * A general purpose visitor that will visit each node in an expression tree.
 * @since 2.0
 */
public interface IExpressionVisitor {
	/**
	 * The method that will be called for each expression that is
	 * visited.
	 * @param expression The expression that the visitor visits.
	 * @return <code>true</code> to continue visiting other expressions or
	 * <code>false</code> to break out.
	 */
	boolean visit(IExpression expression);
}