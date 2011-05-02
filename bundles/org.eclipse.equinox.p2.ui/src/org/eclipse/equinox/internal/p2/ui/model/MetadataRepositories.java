/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * Element class that represents some collection of metadata repositories.
 * It can be configured so that it retrieves its children in different ways.
 * The default query type will return the metadata repositories specified in
 * this element.  Other query types can be used to query each repository and
 * aggregate the children.
 * 
 * @since 3.4
 *
 */
public class MetadataRepositories extends RootElement {

	private boolean includeDisabled = false;

	public MetadataRepositories(ProvisioningUI ui) {
		this(ProvUI.getQueryContext(ui.getPolicy()), ui, null);
	}

	public MetadataRepositories(IUViewQueryContext queryContext, ProvisioningUI ui, QueryableMetadataRepositoryManager queryable) {
		super(queryContext, ui);
		this.queryable = queryable;
	}

	/**
	 * Get whether disabled repositories should be included in queries when no repositories
	 * have been specified.  This boolean is used because the flags specified when getting
	 * repositories from a repository manager are treated as an AND, and we want to permit
	 * aggregating disabled repositories along with other flags.
	 * 
	 * @return includeDisabled <code>true</code> if disabled repositories should be included and
	 * <code>false</code> if they should not be included.  
	 */
	public boolean getIncludeDisabledRepositories() {
		return includeDisabled;
	}

	/**
	 * Set whether disabled repositories should be included in queries when no repositories
	 * have been specified.  This boolean is used because the flags specified when getting
	 * repositories from a repository manager are treated as an AND, and we want to permit
	 * aggregating disabled repositories along with other flags.
	 * 
	 * @param includeDisabled <code>true</code> if disabled repositories should be included and
	 * <code>false</code> if they should not be included.  
	 */
	public void setIncludeDisabledRepositories(boolean includeDisabled) {
		this.includeDisabled = includeDisabled;
	}

	/*
	 * Overridden to check the query context.  We might
	 * be showing repositories, or we might be flattening the 
	 * view to some other element
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#getQueryType()
	 */
	public int getQueryType() {
		if (getQueryContext() == null)
			return getDefaultQueryType();
		return getQueryContext().getQueryType();
	}

	protected int getDefaultQueryType() {
		return QueryProvider.METADATA_REPOS;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	public String getLabel(Object o) {
		return ProvUIMessages.Label_Repositories;
	}

	/*
	 * Overridden because we might be iterating sites 
	 * (type = METADATA_REPOSITORIES) rather than loading repos.  If this
	 * is the case, we only care whether we have a queryable or not.
	 */
	public boolean hasQueryable() {
		if (getQueryType() == QueryProvider.METADATA_REPOS)
			return queryable != null;
		return super.hasQueryable();
	}
}
