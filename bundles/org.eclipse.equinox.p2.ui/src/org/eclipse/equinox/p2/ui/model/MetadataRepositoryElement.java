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

import org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.ui.ProvUIImages;
import org.eclipse.equinox.p2.ui.query.IProvElementQueryProvider;

/**
 * Element wrapper class for a metadata repository that gets its
 * contents in a deferred manner.
 * 
 * @since 3.4
 */
public class MetadataRepositoryElement extends RemoteQueriedElement {

	IMetadataRepository repo;

	public MetadataRepositoryElement(IMetadataRepository repo) {
		this.repo = repo;
		setQueryable(repo);
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IMetadataRepository.class)
			return repo;
		if (adapter == IRepository.class)
			return repo;
		return super.getAdapter(adapter);
	}

	protected String getImageID(Object obj) {
		return ProvUIImages.IMG_METADATA_REPOSITORY;
	}

	protected int getQueryType() {
		return IProvElementQueryProvider.AVAILABLE_IUS;
	}

	public String getLabel(Object o) {
		String name = repo.getName();
		if (name != null && name.length() > 0) {
			return name;
		}
		return repo.getLocation().toExternalForm();

	}
}
