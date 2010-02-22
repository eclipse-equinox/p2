/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
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

import org.eclipse.equinox.internal.p2.metadata.query.IUPropertyQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * A query matching every {@link IInstallableUnit} that is a category.
 * @since 2.0 
 */
public final class CategoryQuery extends ExpressionQuery<IInstallableUnit> {
	private static final String PROP_TYPE_CATEGORY = "org.eclipse.equinox.p2.type.category"; //$NON-NLS-1$

	public CategoryQuery() {
		super(IInstallableUnit.class, IUPropertyQuery.createMatchExpression(PROP_TYPE_CATEGORY, Boolean.TRUE.toString()));
	}

	/**
	 * Test if the {@link IInstallableUnit} is a category. 
	 * @param iu the element being tested.
	 * @return <tt>true</tt> if the parameter is a category.
	 */
	public static boolean isCategory(IInstallableUnit iu) {
		String value = iu.getProperty(PROP_TYPE_CATEGORY);
		if (value != null && (value.equals(Boolean.TRUE.toString())))
			return true;
		return false;
	}
}
