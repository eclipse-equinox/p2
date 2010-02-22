/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.metadata.spi;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.spi.AbstractRepository;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;

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

	public AbstractMetadataRepository(IProvisioningAgent agent) {
		super(agent, "noName", "noType", "noVersion", null, null, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	//TODO Consider removing from abstract class, this is currently an implementation detail of the simple metadata repo parser
	public abstract void initialize(RepositoryState state);

	protected AbstractMetadataRepository(IProvisioningAgent agent, String name, String type, String version, URI location, String description, String provider, Map<String, String> properties) {
		super(agent, name, type, version, location, description, provider, properties);
	}

	// TODO remove
	public void addInstallableUnits(IInstallableUnit[] installableUnit) {
		assertModifiable();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#addInstallableUnits(java.util.Collection)
	 */
	public void addInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		assertModifiable();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#addReference(java.net.URI, java.lang.String, int, int)
	 */
	public void addReference(URI repositoryLocation, String nickname, int repositoryType, int options) {
		assertModifiable();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#removeAll()
	 */
	public void removeAll() {
		assertModifiable();
	}

	// TODO remove
	public boolean removeInstallableUnits(IInstallableUnit[] installableUnits, IProgressMonitor monitor) {
		assertModifiable();
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#removeInstallableUnits(java.util.Collection)
	 */
	public boolean removeInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		assertModifiable();
		return false;
	}

}
