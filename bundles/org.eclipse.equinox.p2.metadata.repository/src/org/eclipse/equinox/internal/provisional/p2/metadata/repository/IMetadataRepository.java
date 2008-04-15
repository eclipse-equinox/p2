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
package org.eclipse.equinox.internal.provisional.p2.metadata.repository;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;

/**
 * A metadata repository stores information about a set of installable units
 * <p>
 * This interface is not intended to be implemented by clients.  Metadata repository
 * implementations must subclass {@link AbstractMetadataRepository} rather than 
 * implementing this interface directly.
 * </p>
 */
public interface IMetadataRepository extends IRepository, IQueryable {

	/** 
	 * Add the given installable units to this repository
	 * @param installableUnits the installable units to add
	 */
	public void addInstallableUnits(IInstallableUnit[] installableUnits);

	/**
	 * Removes all installable units that match the given query from this repository.
	 * 
	 * @param query the installable units to remove
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 * @return <code>true</code> if any units were actually removed, and
	 * <code>false</code> otherwise
	 */
	public boolean removeInstallableUnits(Query query, IProgressMonitor monitor);

	/**
	 * Remove all installable units from this repository.  
	 */
	public void removeAll();

}
