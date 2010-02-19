/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.query;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.query.ExpressionQuery;

/**
 * A query that searches for {@link IInstallableUnit} instances that have
 * a property whose value matches the provided value.  If no property name is 
 * specified, then all {@link IInstallableUnit} instances are accepted.
 */
public final class IUPropertyQuery extends ExpressionQuery<IInstallableUnit> {
	public static final String ANY = "*"; //$NON-NLS-1$

	private static final IExpression matchTrueExpression = ExpressionUtil.parse("properties[$0] == true"); //$NON-NLS-1$
	private static final IExpression matchNullExpression = ExpressionUtil.parse("properties[$0] == null"); //$NON-NLS-1$
	private static final IExpression matchAnyExpression = ExpressionUtil.parse("properties[$0] != null"); //$NON-NLS-1$
	private static final IExpression matchValueExpression = ExpressionUtil.parse("properties[$0] == $1"); //$NON-NLS-1$

	public static IMatchExpression<IInstallableUnit> createMatchExpression(String propertyName, String propertyValue) {
		IExpressionFactory factory = ExpressionUtil.getFactory();
		if (propertyName == null)
			return MATCH_ALL_UNITS;
		if (propertyValue == null)
			return factory.<IInstallableUnit> matchExpression(matchNullExpression, propertyName);
		if (ANY.equals(propertyValue))
			return factory.<IInstallableUnit> matchExpression(matchAnyExpression, propertyName);
		if (Boolean.valueOf(propertyValue).booleanValue())
			return factory.<IInstallableUnit> matchExpression(matchTrueExpression, propertyName);
		return factory.<IInstallableUnit> matchExpression(matchValueExpression, propertyName, propertyValue);
	}

	/**
	 * Creates a new query on the given property name and value.
	 */
	public IUPropertyQuery(String propertyName, String propertyValue) {
		super(IInstallableUnit.class, createMatchExpression(propertyName, propertyValue));
	}
}
