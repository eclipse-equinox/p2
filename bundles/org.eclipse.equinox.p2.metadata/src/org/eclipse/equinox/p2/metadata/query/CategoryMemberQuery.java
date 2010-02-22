/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc. - converted into expression based query
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.query;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.*;

/**
 * A query matching every {@link IInstallableUnit} that is a member
 * of the specified category.
 * 
 * @since 2.0 
 */
public final class CategoryMemberQuery extends ExpressionQuery<IInstallableUnit> {
	private static final IExpression expression = ExpressionUtil.parse("$0.exists(r | this ~= r)"); //$NON-NLS-1$

	private static IMatchExpression<IInstallableUnit> createExpression(IInstallableUnit category) {
		IExpressionFactory factory = ExpressionUtil.getFactory();
		return CategoryQuery.isCategory(category) ? factory.<IInstallableUnit> matchExpression(expression, category.getRequiredCapabilities()) : ExpressionQuery.<IInstallableUnit> matchNothing();
	}

	/**
	 * Creates a new query that will return the members of the
	 * given category.  If the specified {@link IInstallableUnit} 
	 * is not a category, then no installable unit will satisfy the query. 
	 * 
	 * @param category The category
	 */
	public CategoryMemberQuery(IInstallableUnit category) {
		super(IInstallableUnit.class, createExpression(category));
	}
}
