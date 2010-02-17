/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.discovery.repository;

import java.net.URL;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * @author Steffen Pingel
 */
public class RepositorySource extends AbstractCatalogSource {

	private final IMetadataRepository repository;

	public RepositorySource(IMetadataRepository repository) {
		this.repository = repository;
	}

	@Override
	public Object getId() {
		return repository.getLocation();
	}

	@Override
	public URL getResource(String resourceName) {
		return null;
	}

}
