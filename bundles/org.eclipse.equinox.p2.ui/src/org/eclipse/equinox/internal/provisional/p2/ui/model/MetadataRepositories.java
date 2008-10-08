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
package org.eclipse.equinox.internal.provisional.p2.ui.model;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.ElementQueryDescriptor;
import org.eclipse.equinox.internal.provisional.p2.ui.QueryableMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;

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
public class MetadataRepositories extends RemoteQueriedElement {

	private boolean includeDisabled = false;
	private int repoFlags = IRepositoryManager.REPOSITORIES_ALL;
	private IUViewQueryContext queryContext;
	private Policy policy;

	public MetadataRepositories(Policy policy) {
		this(policy.getQueryContext(), policy, null);
	}

	public MetadataRepositories(IUViewQueryContext queryContext, Policy policy, QueryableMetadataRepositoryManager queryable) {
		super(null);
		this.queryContext = queryContext;
		this.policy = policy;
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

	/**
	 * Get the flags that should be used to get the repositories when no repositories
	 * are specified.
	 * 
	 * @return the integer repository manager flags
	 */
	public int getRepoFlags() {
		return repoFlags;
	}

	/**
	 * Set the flags that should be used to get the repositories when no repositories
	 * are specified.
	 * 
	 * @param flags the integer repository manager flags
	 */
	public void setRepoFlags(int flags) {
		this.repoFlags = flags;
	}

	/**
	 * Set the query context that is used when querying the receiver.
	 * 
	 * @param context the query context to use
	 */
	public void setQueryContext(IUViewQueryContext context) {
		queryContext = context;
	}

	public IUViewQueryContext getQueryContext() {
		return queryContext;
	}

	public Policy getPolicy() {
		return policy;
	}

	/*
	 * Overridden to check the query context.  We might
	 * be showing repositories, or we might be flattening the 
	 * view to some other element
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#getQueryType()
	 */
	public int getQueryType() {
		if (queryContext == null)
			return getDefaultQueryType();
		return queryContext.getQueryType();
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
	 * (non-Javadoc)
	 * Overridden because we know that the queryable metadata repo manager can handle a null query
	 * @see org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement#isSufficientForQuery(org.eclipse.equinox.internal.provisional.p2.ui.query.ElementQueryDescriptor)
	 */
	// TODO this is not ideal
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=224504
	protected boolean isSufficientForQuery(ElementQueryDescriptor queryDescriptor) {
		return queryDescriptor.collector != null && queryDescriptor.queryable != null;
	}

	/*
	 * Overridden to check whether the queryable repository manager
	 * has loaded all repositories or not.
	 * This is necessary to prevent background loading of already loaded repositories
	 * by the DeferredTreeContentManager, which will add redundant children to the
	 * viewer.  
	 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=229069
	 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=226343
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#hasQueryable()
	 */
	public boolean hasQueryable() {
		// We use the superclass implementation if we don't have a queryable or
		// don't recognize it.  Also, if we are merely iterating sites rather
		// than loading them to obtain further results, use the superclass
		if (queryable == null || !(queryable instanceof QueryableMetadataRepositoryManager) || getQueryType() == QueryProvider.METADATA_REPOS)
			return super.hasQueryable();
		return ((QueryableMetadataRepositoryManager) queryable).areRepositoriesLoaded();
	}
}
