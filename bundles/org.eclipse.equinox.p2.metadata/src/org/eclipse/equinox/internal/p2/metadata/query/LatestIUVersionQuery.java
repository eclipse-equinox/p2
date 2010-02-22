/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
 *  Cloudsmith Inc. - converted into expression based query
******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.query;

import org.eclipse.equinox.internal.p2.metadata.expression.ContextExpression;
import org.eclipse.equinox.internal.p2.metadata.expression.ExpressionFactory;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.query.ExpressionContextQuery;
import org.eclipse.equinox.p2.query.IQuery;

/**
 * This query returns the latest version for each unique VersionedID.  
 * All other elements are discarded.
 */
public class LatestIUVersionQuery<T extends IVersionedId> extends ExpressionContextQuery<T> {

	private static <T> IContextExpression<T> createLatestExpression(IQuery<T> query) {
		ContextExpression<T> ctxExpr = (ContextExpression<T>) createExpression(query);
		IExpressionFactory factory = ExpressionUtil.getFactory();
		return factory.contextExpression(factory.latest(ctxExpr.operand), ctxExpr.getParameters());
	}

	private static IContextExpression<?> createLatestExpression() {
		IExpressionFactory factory = ExpressionUtil.getFactory();
		return factory.contextExpression(factory.latest(ExpressionFactory.EVERYTHING));
	}

	@SuppressWarnings("unchecked")
	public LatestIUVersionQuery() {
		super((Class<? extends T>) IVersionedId.class, createLatestExpression());
	}

	public LatestIUVersionQuery(IQuery<T> query) {
		super(getElementClass(query), createLatestExpression(query));
	}
}
