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
 * A query matching every {@link IInstallableUnit} that is a group. 
 * @since 2.0
 */
public final class GroupQuery extends ExpressionQuery<IInstallableUnit> {
	private static final String PROP_TYPE_GROUP = "org.eclipse.equinox.p2.type.group"; //$NON-NLS-1$

	public GroupQuery() {
		super(IInstallableUnit.class, IUPropertyQuery.createMatchExpression(PROP_TYPE_GROUP, Boolean.TRUE.toString()));
	}

	/**
	 * Test if the {@link IInstallableUnit} is a group. 
	 * @param iu the element being tested.
	 * @return <tt>true</tt> if the parameter is a group.
	 */
	public static boolean isGroup(IInstallableUnit iu) {
		String value = iu.getProperty(PROP_TYPE_GROUP);
		if (value != null && (value.equals(Boolean.TRUE.toString())))
			return true;
		return false;
	}
}
