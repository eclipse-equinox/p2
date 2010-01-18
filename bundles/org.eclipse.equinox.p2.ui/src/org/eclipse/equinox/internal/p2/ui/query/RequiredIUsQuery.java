/*******************************************************************************
 *  Copyright (c) 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.query;

import java.util.Collection;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.MatchQuery;

/**
/**
 * A query matching every {@link IInstallableUnit} that is meets
 * any requirement of the specified IU.  This query is used when
 * drilling down to show the children of an available IU.  For example,
 * when installing an IU, we want to show all the IU's that are in the provisioning
 * plan that are required by this IU.  This is usually used in combination with
 * other queries (such as show all Required IUs which also are visible to
 * the user as available).
 * 
 * @since 2.0 
 */
public class RequiredIUsQuery extends MatchQuery<IInstallableUnit> {

	private final Collection<IRequirement> required;

	/**
	 * Creates a new query that will return any IU that meets any
	 * one of the requirements of the specified IU. 
	 * 
	 * @param iu The IU whose requirements are to be checked
	 */
	public RequiredIUsQuery(IInstallableUnit iu) {
		this.required = iu.getRequiredCapabilities();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.metadata.query.MatchQuery#isMatch(java.lang.Object)
	 */
	public boolean isMatch(IInstallableUnit candidate) {
		// meeting any requirement is sufficient to match the query.
		for (IRequirement req : required) {
			if (candidate.satisfies(req))
				return true;
		}
		return false;
	}
}
