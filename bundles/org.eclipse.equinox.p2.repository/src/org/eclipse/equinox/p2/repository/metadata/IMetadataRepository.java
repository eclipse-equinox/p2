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
package org.eclipse.equinox.p2.repository.metadata;

import java.net.URI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.IRepository;

/**
 * A metadata repository stores information about a set of installable units
 * <p>
 * This interface is not intended to be implemented by clients.  Metadata repository
 * implementations must subclass {@link AbstractMetadataRepository} rather than 
 * implementing this interface directly.
 * </p>
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface IMetadataRepository extends IRepository {

	/** 
	 * Add the given installable units to this repository
	 * @param installableUnits the installable units to add
	 */
	public void addInstallableUnits(IInstallableUnit[] installableUnits);

	/**
	 * Adds a reference to another repository to this repository. When a repository
	 * is loaded by {@link IMetadataRepositoryManager}, its references
	 * are automatically added to the repository manager's set of known repositories.
	 * <p>
	 * Note that this method does not add the <b>contents</b> of the given
	 * repository to this repository, but merely adds the location of another
	 * repository to the metadata of this repository.
	 * <p>
	 * The {@link IRepository#ENABLED} option flag controls whether the 
	 * referenced repository should be marked as enabled when added to the repository
	 * manager. If this flag is set, the repository will be marked as enabled when
	 * added to the repository manager. If this flag is missing, the repository will
	 * be marked as disabled.
	 * 
	 * @param location the location of the repository to add
	 * @param nickname The nickname of the repository, or <code>null</code>
	 * @param type the repository type (currently either {@link IRepository#TYPE_METADATA}
	 * or {@link IRepository#TYPE_ARTIFACT}).
	 * @param options bit-wise or of option constants (currently either 
	 * {@link IRepository#ENABLED} or {@link IRepository#NONE}).
	 * @see IMetadataRepositoryManager#setEnabled(URI, boolean)
	 */
	public void addReference(URI location, String nickname, int type, int options);

	/**
	 * Removes all installable units that match the given query from this repository.
	 * 
	 * @param query the installable units to remove
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return <code>true</code> if any units were actually removed, and
	 * <code>false</code> otherwise
	 */
	public boolean removeInstallableUnits(IInstallableUnit[] installableUnits, IProgressMonitor monitor);

	/**
	 * Remove all installable units from this repository.  
	 */
	public void removeAll();

}
