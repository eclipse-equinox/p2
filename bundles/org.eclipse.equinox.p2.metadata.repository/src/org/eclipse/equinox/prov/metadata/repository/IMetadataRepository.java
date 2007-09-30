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
package org.eclipse.equinox.prov.metadata.repository;

import java.net.URL;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.prov.core.repository.IRepositoryInfo;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.query.IQueryable;

/**
 * A metadata repository stores information about a set of installable units
 * <p>
 * Clients may implement this interface.
 * </p>
 * TODO: This should be an abstract class so methods can be added in the future
 * without breaking clients.
 */
public interface IMetadataRepository extends IRepositoryInfo, IQueryable {
	/**
	 * Returns all installable units known to this repository.
	 * @param monitor TODO
	 * @return the installable units known to this repository
	 *TODO: Progress monitor? Is the repository expected to be local?
	 */
	public IInstallableUnit[] getInstallableUnits(IProgressMonitor monitor);

	/**
	 * Returns the URL of this repository.
	 * TODO: Should we use URL or URI? URL requires a protocol handler to be installed
	 * in Java.  Can the URL have any protocol?  Why are we exposing this at all?
	 * @return the URL of this repository.
	 */
	public URL getLocation();
}
