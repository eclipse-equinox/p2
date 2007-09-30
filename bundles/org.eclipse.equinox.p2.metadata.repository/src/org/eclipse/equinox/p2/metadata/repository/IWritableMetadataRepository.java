/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Prashant Deva - Bug 194674 [prov] Provide write access to metadata repository
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.repository;

import org.eclipse.equinox.p2.core.repository.IWritableRepositoryInfo;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public interface IWritableMetadataRepository extends IMetadataRepository, IWritableRepositoryInfo {

	void addInstallableUnits(IInstallableUnit[] installableUnit);

	void removeInstallableUnits(IInstallableUnit[] installableUnit);

	/**
	 * Remove IUs from this repository.  
	 */
	public void removeAll();

}
