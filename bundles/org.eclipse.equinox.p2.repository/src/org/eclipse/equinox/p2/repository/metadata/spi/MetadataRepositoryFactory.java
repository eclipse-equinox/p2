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
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * A metadata repository factory is responsible for creating and loading instances
 * of a particular type of metadata repository. Factories are provided via the 
 * <tt>org.eclipse.equinox.p2.metadata.repository.metadataRepositories</tt> extension point.
 * @since 2.0
 */
public abstract class MetadataRepositoryFactory {
	private IProvisioningAgent agent;

	/**
	 * Creates and returns a new empty metadata repository of the given type at 
	 * the given location.
	 * 
	 * @param location the location for the new repository
	 * @param name the name of the new repository
	 * @param type the kind of repository to create
	 * @param properties the properties to set on the repository
	 * @return the newly created repository
	 * @throws ProvisionException if the repository could not be created.  Reasons include:
	 * <ul>
	 * <li>The repository type is not supported by this factory.</li>
	 * <li>There was an error writing to the given repository location.</li>
	 * </ul>
	 */
	public abstract IMetadataRepository create(URI location, String name, String type, Map<String, String> properties) throws ProvisionException;

	/**
	 * Returns the provisioning agent associated with this factory, or <code>null</code>
	 * if this factory is not associated with an agent.
	 * @return The provisioning agent, or <code>null</code>
	 */
	protected IProvisioningAgent getAgent() {
		return agent;
	}

	/**
	 * Loads a repository corresponding to the given URL.
	 * <p>
	 * The error code returned in the case of failure is significant. In particular an
	 * error code of {@link ProvisionException#REPOSITORY_FAILED_READ} indicates
	 * that the location definitely identifies a repository of this type, but an error occurred
	 * while loading the repository. The repository manager will not attempt to load
	 * a repository from that location using any other factory.  An error code of
	 * {@link ProvisionException#REPOSITORY_NOT_FOUND} indicates there is no
	 * repository of this type at the given location, and the repository manager is free
	 * to try again with a different repository factory.
	 * </p>
	 * <p>
	 * The flags passed in should be taken as a hint for the type of repository to load.  If
	 * the factory knows it will not load a repository that satisfies these hints, it can fail
	 * fast and return null.<br>
	 * See {@link IRepositoryManager#REPOSITORY_HINT_MODIFIABLE}
	 * </p>
	 * @param location The location of the repository to load
	 * @param flags to consider while loading the repository
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The loaded metadata repository
	 * @throws ProvisionException if the repository could not be created.  Reasons include:
	 * <ul>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 */
	public abstract IMetadataRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException;

	/**
	 * Sets the provisioning agent associated with this repository factory.
	 * @param agent The provisioning agent
	 */
	public void setAgent(IProvisioningAgent agent) {
		this.agent = agent;
	}
}
