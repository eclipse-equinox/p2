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
package org.eclipse.equinox.p2.metadata.repository;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;

/**
 * A metadata repository stores information about a set of installable units
 * <p>
 * Clients may implement this interface.
 * </p>
 */
public interface IMetadataRepository extends IRepository, IQueryable {

	/**
	 * Returns all installable units known to this repository.
	 * @param monitor TODO
	 * @return the installable units known to this repository
	 */
	public IInstallableUnit[] getInstallableUnits(IProgressMonitor monitor);

	/** 
	 * Add the given installable units to this repository
	 * @param installableUnits the installable unts to add
	 */
	public void addInstallableUnits(IInstallableUnit[] installableUnits);

	/**
	 * Remove the given installable units from this repository
	 * @param installableUnits the installable units to remove
	 */
	public void removeInstallableUnits(IInstallableUnit[] installableUnits);

	/**
	 * Remove all installable units from this repository.  
	 */
	public void removeAll();

}
