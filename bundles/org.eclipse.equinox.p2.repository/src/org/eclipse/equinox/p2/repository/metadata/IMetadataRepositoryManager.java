/*******************************************************************************
 * Copyright (c) 2007, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.metadata;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.IRepositoryManager;

/**
 * A metadata repository manager is used to create, access, and manipulate
 * {@link IMetadataRepository} instances. See {@link IRepositoryManager} for a
 * general description of the characteristics of repository managers.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IMetadataRepositoryManager extends IRepositoryManager<IInstallableUnit> {
	/**
	 * The name used for obtaining a reference to the metadata repository manager service
	 */
	String SERVICE_NAME = IMetadataRepositoryManager.class.getName();

	/**
	 * Repository type for a simple repository based on a URL or local file system location.
	 */
	String TYPE_SIMPLE_REPOSITORY = "org.eclipse.equinox.p2.metadata.repository.simpleRepository"; //$NON-NLS-1$

	/**
	 * Repository type for a composite repository based on a URL or local file system location.
	 */
	String TYPE_COMPOSITE_REPOSITORY = "org.eclipse.equinox.p2.metadata.repository.compositeRepository"; //$NON-NLS-1$

	/**
	 * Creates and returns a new empty metadata repository of the given type at
	 * the given location.
	 * <p>
	 * The resulting repository is added to the list of repositories tracked by
	 * the repository manager. Clients must make a subsequent call to {@link #removeRepository(URI)}
	 * if they do not want the repository manager to remember the repository for subsequent
	 * load attempts.
	 * </p>
	 *
	 * @param location the absolute location for the new repository
	 * @param name the name of the new repository
	 * @param type the kind of repository to create
	 * @param properties the properties to set on the repository
	 * @return the newly created repository
	 * @throws ProvisionException if the repository could not be created. Reasons include:
	 * <ul>
	 * <li>The repository type is unknown.</li>
	 * <li>There was an error writing to the given repository location.</li>
	 * <li>A repository already exists at that location.</li>
	 * </ul>
	 * @throws OperationCanceledException if this operation has been canceled
	 */
	@Override
	IMetadataRepository createRepository(URI location, String name, String type, Map<String, String> properties)
			throws ProvisionException, OperationCanceledException;

	/**
	 * Loads the repository at the given location. If a repository has
	 * previously been loaded at that location, the same cached repository
	 * may be returned.
	 * <p>
	 * The resulting repository is added to the list of repositories tracked by
	 * the repository manager. Clients must make a subsequent call to {@link #removeRepository(URI)}
	 * if they do not want the repository manager to remember the repository for subsequent
	 * load attempts.
	 * </p>
	 *
	 * @param location the absolute location of the repository to load
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return the loaded metadata repository
	 * @throws OperationCanceledException if this operation has been canceled
	 * @throws ProvisionException if the repository could not be created. Reasons include:
	 * <ul>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 */
	@Override
	IMetadataRepository loadRepository(URI location, IProgressMonitor monitor)
			throws ProvisionException, OperationCanceledException;

	/**
	 * Loads the repository at the given location. If a repository has
	 * previously been loaded at that location, the same cached repository may be returned.
	 * <p>
	 * The resulting repository is added to the list of repositories tracked by
	 * the repository manager. Clients must make a subsequent call to {@link #removeRepository(URI)}
	 * if they do not want the repository manager to remember the repository for subsequent
	 * load attempts.
	 * </p>
	 * <p>
	 * The flags passed in should be taken as a hint for the type of repository to load. If
	 * the manager cannot load a repository that satisfies these hints, it can fail fast.
	 * </p>
	 *
	 * @param location the absolute location of the repository to load
	 * @param flags bit-wise or of flags to consider when loading the repository
	 *  (currently only {@link IRepositoryManager#REPOSITORY_HINT_MODIFIABLE} is supported)
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return the loaded metadata repository
	 * @throws OperationCanceledException if this operation has been canceled
	 * @throws ProvisionException if the repository could not be created. Reasons include:
	 * <ul>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 * @see IRepositoryManager#REPOSITORY_HINT_MODIFIABLE
	 */
	@Override
	IMetadataRepository loadRepository(URI location, int flags, IProgressMonitor monitor)
			throws ProvisionException, OperationCanceledException;

	/**
	 * Refreshes the repository corresponding to the given URL. This method discards
	 * any cached state held by the repository manager and reloads the repository
	 * contents. The provided repository location must already be known to the repository
	 * manager.
	 *
	 * @param location The absolute location of the repository to refresh
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return The refreshed metadata repository
	 * @throws OperationCanceledException if this operation has been canceled
	 * @throws ProvisionException if the repository could not be refreshed. Reasons include:
	 * <ul>
	 * <li>The location is not known to the repository manager.</li>
	 * <li>There is no existing repository at that location.</li>
	 * <li>The repository at that location could not be read.</li>
	 * </ul>
	 */
	@Override
	IMetadataRepository refreshRepository(URI location, IProgressMonitor monitor)
			throws ProvisionException, OperationCanceledException;

}
