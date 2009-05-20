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
package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

/**
 * Operation that removes the colocated repositories with the given locations. *
 * 
 * @since 3.4
 */
public class RemoveColocatedRepositoryOperation extends RemoveRepositoryOperation {

	public RemoveColocatedRepositoryOperation(String label, URI[] repoLocations) {
		super(label, repoLocations);
	}

	protected IStatus doBatchedExecute(IProgressMonitor monitor) throws ProvisionException {
		SubMonitor mon = SubMonitor.convert(monitor, locations.length * 2);

		for (int i = 0; i < locations.length; i++) {
			ProvisioningUtil.removeMetadataRepository(locations[i]);
			mon.worked(1);
			ProvisioningUtil.removeArtifactRepository(locations[i]);
			mon.worked(1);
		}
		return okStatus();
	}
}
