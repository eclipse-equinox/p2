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
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.query.ElementQueryDescriptor;

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
	private int queryType = IQueryProvider.METADATA_REPOS;

	public MetadataRepositories() {
		super();
	}

	public MetadataRepositories(URL[] metadataRepositories) {
		this.metadataRepositories = metadataRepositories;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#getQueryType()
	 */
	protected int getQueryType() {
		return queryType;
	}

	public void setQueryType(int queryType) {
		this.queryType = queryType;
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

}
