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

import java.util.List;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.MatchQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

/**
/**
 * A query matching every {@link IInstallableUnit} that is a member
 * of the specified category.
 * 
 * @since 2.0 
 */
public class CategoryMemberQuery extends MatchQuery<IInstallableUnit> {
	private final List<IRequirement> required;

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
		int top = required.size();
		for (int i = 0; i < top; i++)
			if (candidate.satisfies(required.get(i)))
				return true;
		return false;
	}
}
