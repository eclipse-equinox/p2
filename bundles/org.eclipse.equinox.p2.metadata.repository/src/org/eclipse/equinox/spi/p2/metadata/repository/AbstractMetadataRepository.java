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
package org.eclipse.equinox.spi.p2.metadata.repository;

import java.net.URL;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.spi.p2.core.repository.AbstractRepository;

public abstract class AbstractMetadataRepository extends AbstractRepository implements IMetadataRepository {

	protected AbstractMetadataRepository(String name, String type, String version, URL location, String description, String provider) {
		super(name, type, version, location, description, provider);
	}

	public void addInstallableUnits(IInstallableUnit[] installableUnit) {
		if (!isModifiable())
			throw new UnsupportedOperationException("Repository not modifiable");
	}

	public void removeAll() {
		if (!isModifiable())
			throw new UnsupportedOperationException("Repository not modifiable");
	}

	public void removeInstallableUnits(IInstallableUnit[] installableUnit) {
		if (!isModifiable())
			throw new UnsupportedOperationException("Repository not modifiable");
	}

}
