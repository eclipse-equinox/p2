/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.repository.Activator;
import org.eclipse.equinox.p2.core.IPool;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;
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

	/**
	 * A class that encapsulates the persisted state of a repository. This is used as a convenience
	 * when loading and storing repositories.
	 * @see AbstractMetadataRepository#initialize(RepositoryState)
	 */
	public static class RepositoryState {
		/**
		 * The persisted name of the repository.
		 */
		public String Name;
		/**
		 * The persisted type of the repository.
		 */
		public String Type;
		/**
		 * The persisted version of the repository.
		 */
		public Version Version;
		/**
		 * The persisted provider of the repository.
		 */
		public String Provider;
		/**
		 * The persisted description of the repository.
		 */
		public String Description;
		/**
		 * The persisted location of the repository.
		 */
		public URI Location;
		/**
		 * The persisted properties of the repository.
		 */
		public Map<String, String> Properties;
		/**
		 * The persisted set of installable units of the repository.
		 */
		public IInstallableUnit[] Units;
		/**
		 * The persisted array of repository references
		 */
		public IRepositoryReference[] Repositories;
	}

	/**
	 * Creates a new metadata repository that uses the provided agent.
	 * 
	 * @param agent the provisioning agent to be used by this repository
	 */
	public AbstractMetadataRepository(IProvisioningAgent agent) {
		super(agent, "noName", "noType", "noVersion", null, null, null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Initializes this class based on the provided previously persisted state
	 * 
	 * @param state the persisted repository state
	 */
	public abstract void initialize(RepositoryState state);

	/**
	 * Creates a new metadata repository with the provided repository information
	 * 
	 * @param agent the provisioning agent to be used by this repository
	 * @param name the repository name
	 * @param type the repository type
	 * @param version the repository version
	 * @param location the repository location
	 * @param description the repository description
	 * @param provider the repository provider
	 * @param properties the repository properties
	 */
	protected AbstractMetadataRepository(IProvisioningAgent agent, String name, String type, String version, URI location, String description, String provider, Map<String, String> properties) {
		super(agent, name, type, version, location, description, provider, properties);
	}

	/**
	 * {@inheritDoc}
	 */
	public void addInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		assertModifiable();
	}

	/**
	 * {@inheritDoc}
	 */
	public void addReferences(Collection<? extends IRepositoryReference> references) {
		assertModifiable();
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAll() {
		assertModifiable();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean removeInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		assertModifiable();
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
		try {
			runnable.run(monitor);
		} catch (OperationCanceledException oce) {
			return new Status(IStatus.CANCEL, Activator.ID, oce.getMessage(), oce);
		} catch (Exception e) {
			return new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e);
		}
		return Status.OK_STATUS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#compress(IPool<IInstallableUnit> iuPool)
	 */
	/**
	 * @since 2.1
	 */
	public void compress(IPool<IInstallableUnit> iuPool) {
		// Default no-op.  Subclasses should override as appropriate
	}

}
