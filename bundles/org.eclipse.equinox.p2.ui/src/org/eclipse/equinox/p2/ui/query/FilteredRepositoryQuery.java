/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.query;

import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.query.Query;

/**
 * A query that provides flags for filtering out which repositories
 * are of interest.  This query is used for optimizations so that only
 * those repository URLs that meet the filter flags will be iterated
 * in the query.  This can prevent unnecessary loading a repositories when
 * only a subset of repositories is desired for running a query.
 * <p>
 * This query may be used alone, or in conjunction with
 * other queries that actually load the repository and further refine
 * the repositories of interest.
 */
public class FilteredRepositoryQuery extends Query {
	private int flags = IMetadataRepositoryManager.REPOSITORIES_ALL;

	/**
	 * Creates a new query which uses the provided flags to filter out
	 * repositories before loading and querying them. 
	 */
	public FilteredRepositoryQuery(int flags) {
		this.flags = flags;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.query2.Query#isMatch(java.lang.Object)
	 */
	public boolean isMatch(Object object) {
		return true;
	}

	public int getFlags() {
		return flags;
	}
}
