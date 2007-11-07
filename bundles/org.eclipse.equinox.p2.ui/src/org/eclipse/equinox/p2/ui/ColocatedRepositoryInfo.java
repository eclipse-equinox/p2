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

package org.eclipse.equinox.p2.ui;

import org.eclipse.equinox.p2.core.OrderedProperties;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.spi.p2.core.repository.AbstractRepository;

/**
 * Repository info for a colocated repository.
 * 
 * @since 3.4
 */
public class ColocatedRepositoryInfo extends AbstractRepository {

	private IMetadataRepository repo;

	public ColocatedRepositoryInfo(IMetadataRepository repo) {
		super(repo.getName(), repo.getType(), repo.getVersion(), ColocatedRepositoryUtil.makeColocatedRepositoryURL(repo.getLocation()), repo.getDescription(), repo.getProvider());
		this.repo = repo;
	}

	public String getDescription() {
		return repo.getDescription();
	}

	public String getName() {
		return repo.getName();
	}

	public OrderedProperties getProperties() {
		return repo.getProperties();
	}

	public String getProvider() {
		return repo.getProvider();
	}

	public Object getAdapter(Class adapter) {
		return repo.getAdapter(adapter);
	}
}
