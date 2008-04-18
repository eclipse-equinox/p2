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
package org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository;

import java.net.URL;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.core.repository.AbstractRepository;
import org.osgi.framework.Version;

/**
 * The common base class for all metadata repositories.
 * <p>
 * Clients may subclass this class.
 * <p>
 */
public abstract class AbstractMetadataRepository extends AbstractRepository implements IMetadataRepository {

	public static class RepositoryState {
		public String Name;
		public String Type;
		public Version Version;
		public String Provider;
		public String Description;
		public URL Location;
		public Map Properties;
		public IInstallableUnit[] Units;
	}

	public AbstractMetadataRepository() {
		super("noName", "noType", "noVersion", null, null, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public abstract void initialize(RepositoryState state);

	protected AbstractMetadataRepository(String name, String type, String version, URL location, String description, String provider, Map properties) {
		super(name, type, version, location, description, provider, properties);
	}

	public void addInstallableUnits(IInstallableUnit[] installableUnit) {
		if (!isModifiable())
			throw new UnsupportedOperationException("Repository not modifiable");
	}

	public void removeAll() {
		if (!isModifiable())
			throw new UnsupportedOperationException("Repository not modifiable");
	}

	public boolean removeInstallableUnits(Query query, IProgressMonitor monitor) {
		if (!isModifiable())
			throw new UnsupportedOperationException("Repository not modifiable");
		return false;
	}

}
