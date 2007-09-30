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

package org.eclipse.equinox.prov.ui;

import java.net.URL;
import org.eclipse.equinox.prov.core.helpers.UnmodifiableProperties;
import org.eclipse.equinox.prov.core.repository.IRepositoryInfo;
import org.eclipse.equinox.prov.metadata.repository.IMetadataRepository;

/**
 * Repository info for a colocated repository.
 * 
 * @since 3.4
 */
public class ColocatedRepositoryInfo implements IRepositoryInfo {

	private IMetadataRepository repo;

	public ColocatedRepositoryInfo(IMetadataRepository repo) {
		this.repo = repo;
	}

	public String getDescription() {
		return repo.getDescription();
	}

	public String getName() {
		return repo.getName();
	}

	public UnmodifiableProperties getProperties() {
		return repo.getProperties();
	}

	public String getProvider() {
		return repo.getProvider();
	}

	public String getType() {
		return repo.getType();
	}

	public URL getLocation() {
		return ColocatedRepositoryUtil.makeColocatedRepositoryURL(repo.getLocation());
	}

	public String getVersion() {
		return repo.getVersion();
	}

	public Object getAdapter(Class adapter) {
		return repo.getAdapter(adapter);
	}
}
