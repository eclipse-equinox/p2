/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
*   Cloudsmith Inc. - converted into expression based query
******************************************************************************/
package org.eclipse.equinox.p2.query;

import org.eclipse.equinox.internal.p2.metadata.expression.ContextExpression;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.query.ExpressionContextQuery;

/**
 * A limit query can be used to limit the number of query results returned.  Once
 * the limit is reached, the query is terminated.
 * @since 2.0
 */
public class LimitQuery<T> extends ExpressionContextQuery<T> {

	private static <T> IContextExpression<T> createLimitExpression(IQuery<T> query, int limit) {
		ContextExpression<T> ctxExpr = (ContextExpression<T>) createExpression(query);
		IExpressionFactory factory = ExpressionUtil.getFactory();
		return factory.contextExpression(factory.limit(ctxExpr.operand, limit), ctxExpr.getParameters());
	}

	public LimitQuery(IQuery<T> query, int limit) {
		super(getElementClass(query), createLimitExpression(query, limit));
	}
}
