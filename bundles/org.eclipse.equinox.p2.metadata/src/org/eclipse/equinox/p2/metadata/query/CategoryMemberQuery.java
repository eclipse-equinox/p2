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
package org.eclipse.equinox.p2.metadata.query;

import java.util.Collection;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.MatchQuery;

/**
/**
 * A query matching every {@link IInstallableUnit} that is a member
 * of the specified category.
 * 
 * @since 2.0 
 */
public class CategoryMemberQuery extends MatchQuery<IInstallableUnit> {
	private final Collection<IRequirement> required;

	/**
	 * Creates a new query that will return the members of the
	 * given category.  If the specified {@link IInstallableUnit} 
	 * is not a category, then no installable unit will satisfy the query. 
	 * 
	 * @param category The category
	 */
	public CategoryMemberQuery(IInstallableUnit category) {
		if (CategoryQuery.isCategory(category))
			this.required = category.getRequiredCapabilities();
		else
			this.required = CollectionUtils.emptyList();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.metadata.query.MatchQuery#isMatch(java.lang.Object)
	 */
	public boolean isMatch(IInstallableUnit candidate) {
		// since a category lists its members as requirements, then meeting
		// any requirement means the candidate is a member of the category.
		for (IRequirement req : required) {
			if (candidate.satisfies(req))
				return true;
		}
		return false;
	}
}
