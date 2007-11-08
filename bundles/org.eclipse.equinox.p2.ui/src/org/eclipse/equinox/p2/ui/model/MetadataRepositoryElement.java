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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.ui.ProvUIImages;

/**
 * Element wrapper class for a metadata repository that gets its
 * contents in a deferred manner.
 * 
 * @since 3.4
 */
public class MetadataRepositoryElement extends RepositoryElement {

	public MetadataRepositoryElement(IMetadataRepository repo) {
		super(repo);
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IMetadataRepository.class)
			return repo;
		return super.getAdapter(adapter);
	}

	protected String getImageID(Object obj) {
		return ProvUIImages.IMG_METADATA_REPOSITORY;
	}

	protected Object[] fetchChildren(Object o, IProgressMonitor monitor) {
		return ((IMetadataRepository) repo).getInstallableUnits(monitor);

	}
}
