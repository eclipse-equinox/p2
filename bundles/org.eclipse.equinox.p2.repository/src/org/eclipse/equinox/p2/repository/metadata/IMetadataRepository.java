/*******************************************************************************
 *  Copyright (c) 2007, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.metadata;

import java.util.Collection;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IPool;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;

/**
 * A metadata repository stores information about a set of installable units
 * <p>
 * This interface is not intended to be implemented by clients.  Metadata repository
 * implementations must subclass {@link AbstractMetadataRepository} rather than
 * implementing this interface directly.
 * </p>
 * @noimplement This interface is not intended to be implemented by clients. Instead subclass {@link AbstractMetadataRepository}
 * @since 2.0
 */
public interface IMetadataRepository extends IRepository<IInstallableUnit> {

	/**
	 * Add the given installable units to this repository.
	 *
	 * @param installableUnits the installable units to add
	 */
	void addInstallableUnits(Collection<IInstallableUnit> installableUnits);

	/**
	 * <p>Adds references to another repository to this repository. When a repository
	 * is loaded by {@link IMetadataRepositoryManager}, its references
	 * are automatically added to the repository manager's set of known repositories.</p>
	 * <p>Note that this method does not add the <b>contents</b> of the given
	 * repositories to this repository, but merely adds the location of other
	 * repositories to the metadata of this repository.</p>
	 *
	 * @param references The references to add
	 */
	void addReferences(Collection<? extends IRepositoryReference> references);

	/**
	 * Returns the repositories that this repository is referencing.
	 * @return An immutable collection of repository references, possibly empty but never <code>null</code>.
	 */
	Collection<IRepositoryReference> getReferences();

	/**
	 * Removes from this repository the given references to other repositories.
	 *
	 * @since 2.8
	 */
	boolean removeReferences(Collection<? extends IRepositoryReference> references);

	/**
	 * Removes all installable units in the given collection from this repository.
	 *
	 * @param installableUnits the installable units to remove
	 * @return <code>true</code> if any units were actually removed, and
	 * <code>false</code> otherwise
	 */
	boolean removeInstallableUnits(Collection<IInstallableUnit> installableUnits);

	/**
	 * Remove all installable units from this repository.
	 */
	void removeAll();

	/**
	 * Executes a runnable against this repository. It is up to the repository
	 * implementor to determine what "batch process" means, for example, it may mean
	 * that the repository index is not stored until after the runnable completes.
	 *
	 * The runnable should not execute anything in a separate thread.
	 *
	 * @param runnable The runnable to execute
	 * @param monitor A progress monitor that will be passed to the runnable
	 * @return The result of running the runnable. Any exceptions thrown during
	 * the execution will be returned in the status.
	 */
	IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor);

	/**
	 * Cause semantically equivalent IInstallableUnits in the receiver to be
	 * replaced with a shared object from the provided {@link IPool}.  New objects are
	 * added to the {@link IPool} as required.
	 * <p>
	 * While the {@link IPool} should be retained to increase the scope of sharing when
	 * calling {@link #compress(IPool)} on subsequent repositories, the {@link IPool} can
	 * be discarded without adversely effecting the receiver.
	 * </p>
	 * @since 2.1
	 */
	void compress(IPool<IInstallableUnit> iuPool);
}
