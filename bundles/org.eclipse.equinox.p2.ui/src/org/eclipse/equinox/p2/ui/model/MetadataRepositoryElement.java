/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.model;

import java.net.URL;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.ProvUIImages;
import org.eclipse.equinox.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.p2.ui.query.IProvElementQueryProvider;
import org.eclipse.osgi.util.NLS;

/**
 * Element wrapper class for a metadata repository that gets its
 * contents in a deferred manner.
 * 
 * @since 3.4
 */
public class MetadataRepositoryElement extends RemoteQueriedElement implements RepositoryElement {

	URL url;

	public MetadataRepositoryElement(URL url) {
		this.url = url;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IMetadataRepository.class)
			return getQueryable();
		if (adapter == IRepository.class)
			return getQueryable();
		return super.getAdapter(adapter);
	}

	protected String getImageID(Object obj) {
		return ProvUIImages.IMG_METADATA_REPOSITORY;
	}

	protected int getQueryType() {
		return IProvElementQueryProvider.AVAILABLE_IUS;
	}

	public String getLabel(Object o) {
		String name = getName();
		if (name != null && name.length() > 0) {
			return name;
		}
		return getURL().toExternalForm();
	}

	/*
	 * overridden to lazily fetch repository
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.ui.query.QueriedElement#getQueryable()
	 */
	public IQueryable getQueryable() {
		if (queryable == null)
			try {
				queryable = ProvisioningUtil.loadMetadataRepository(url, null);
			} catch (ProvisionException e) {
				ProvUI.handleException(e, NLS.bind(ProvUIMessages.MetadataRepositoryElement_RepositoryLoadError, url));
			}
		return queryable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.ui.model.RepositoryElement#getURL()
	 */
	public URL getURL() {
		return url;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.ui.model.RepositoryElement#getName()
	 */
	public String getName() {
		try {
			return ProvisioningUtil.getMetadataRepositoryName(url);
		} catch (ProvisionException e) {
			return ""; //$NON-NLS-1$
		}
	}
}
