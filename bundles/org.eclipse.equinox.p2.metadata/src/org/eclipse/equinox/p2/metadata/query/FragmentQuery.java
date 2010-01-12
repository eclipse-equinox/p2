/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.query;

import org.eclipse.equinox.internal.p2.metadata.query.IUPropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.MatchQuery;

/**
 * A query matching every {@link IInstallableUnit} that is a category.
 * @since 2.0
 */
public final class FragmentQuery extends MatchQuery<IInstallableUnit> {
	private static final String PROP_TYPE_FRAGMENT = "org.eclipse.equinox.p2.type.fragment"; //$NON-NLS-1$
	private IUPropertyQuery query;

	public FragmentQuery() {
		query = new IUPropertyQuery(PROP_TYPE_FRAGMENT, null);
	}

	public boolean isMatch(IInstallableUnit candidate) {
		return query.isMatch(candidate);
	}

	/**
	 * Test if the {@link IInstallableUnit} is a fragment. 
	 * @param iu the element being tested.
	 * @return <tt>true</tt> if the parameter is a fragment.
	 */
	public static boolean isFragment(IInstallableUnit iu) {
		if (iu instanceof IInstallableUnitFragment)
			return true;
		//		String value = iu.getProperty(PROP_TYPE_FRAGMENT);
		//		if (value != null && (value.equals(Boolean.TRUE.toString())))
		//			return true;
		return false;
	}
}
