/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.metadata.spi;

import org.eclipse.equinox.p2.repository.spi.RepositoryReference;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.spi.AbstractRepository;

/**
 * The common base class for all metadata repositories.
 * <p>
 * Clients may subclass this class.
 * <p>
 * @since 2.0
 */
public abstract class AbstractMetadataRepository extends AbstractRepository<IInstallableUnit> implements IMetadataRepository {

	//TODO Consider removing from abstract class, this is currently an implementation detail of the simple metadata repo parser
	public static class RepositoryState {
		public String Name;
		public String Type;
		public Version Version;
		public String Provider;
		public String Description;
		public URI Location;
		public Map<String, String> Properties;
		public IInstallableUnit[] Units;
		public RepositoryReference[] Repositories;
	}

	public AbstractMetadataRepository() {
		super("noName", "noType", "noVersion", null, null, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	//TODO Consider removing from abstract class, this is currently an implementation detail of the simple metadata repo parser
	public abstract void initialize(RepositoryState state);

	protected AbstractMetadataRepository(String name, String type, String version, URI location, String description, String provider, Map<String, String> properties) {
		super(name, type, version, location, description, provider, properties);
	}

	public void addInstallableUnits(IInstallableUnit[] installableUnit) {
		assertModifiable();
	}

	public void addReference(URI repositoryLocation, String nickname, int repositoryType, int options) {
		assertModifiable();
	}

	public void removeAll() {
		assertModifiable();
	}

	public boolean removeInstallableUnits(IInstallableUnit[] installableUnits, IProgressMonitor monitor) {
		assertModifiable();
		return false;
	}

}
