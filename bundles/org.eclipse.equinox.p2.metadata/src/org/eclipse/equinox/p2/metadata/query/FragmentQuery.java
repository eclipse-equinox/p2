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

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;

/**
 * A query matching every {@link IInstallableUnit} that is a category.
 * @since 2.0
 */
public final class FragmentQuery extends ExpressionQuery<IInstallableUnit> {
	public FragmentQuery() {
		super(IInstallableUnitFragment.class, matchAll());
	}

	/**
	 * Test if the {@link IInstallableUnit} is a fragment. 
	 * @param iu the element being tested.
	 * @return <tt>true</tt> if the parameter is a fragment.
	 */
	public static boolean isFragment(IInstallableUnit iu) {
		return iu instanceof IInstallableUnitFragment;
		//		String value = iu.getProperty(PROP_TYPE_FRAGMENT);
		//		return value != null && (value.equals(Boolean.TRUE.toString()));
	}
}
