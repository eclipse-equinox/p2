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
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement;

/**
 * Element class that represents the root of a metadata
 * repository viewer.  Its children are obtained using the
 * metadata repository query specified by the client in the
 * content provider.
 * 
 * @since 3.4
 *
 */
public class MetadataRepositories extends QueriedElement {

	private URL[] metadataRepositories = null;

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

}
