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

import java.net.URL;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.query.ElementQueryDescriptor;
import org.eclipse.equinox.internal.provisional.p2.ui.query.QueryableMetadataRepositoryManager;

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

	private URL[] metadataRepositories = null;
	private boolean includeDisabled = false;
	private int repoFlags = IMetadataRepositoryManager.REPOSITORIES_ALL;

	public MetadataRepositories() {
		super();
	}

	public MetadataRepositories(URL[] metadataRepositories) {
		this.metadataRepositories = metadataRepositories;
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

	/*
	 * Overridden to check the query context.  We might
	 * be showing repositories, or we might be flattening the 
	 * view to some other element
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#getQueryType()
	 */
	protected int getQueryType() {
		if (queryContext == null)
			return getDefaultQueryType();
		return queryContext.getQueryType();
	}

	protected int getDefaultQueryType() {
		return IQueryProvider.METADATA_REPOS;
	}

	/**
	 * Return the array of URLs for the metadata repositories that
	 * this element represents.  A value of <code>null</code> means
	 * all repositories are represented.  
	 * 
	 * @return the array of repositories, or <code>null</code>.
	 */
	public URL[] getMetadataRepositories() {
		return metadataRepositories;
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
		if (queryable == null || !(queryable instanceof QueryableMetadataRepositoryManager) || getQueryType() == IQueryProvider.METADATA_REPOS)
			return super.hasQueryable();
		return ((QueryableMetadataRepositoryManager) queryable).areRepositoriesLoaded();
	}
}
