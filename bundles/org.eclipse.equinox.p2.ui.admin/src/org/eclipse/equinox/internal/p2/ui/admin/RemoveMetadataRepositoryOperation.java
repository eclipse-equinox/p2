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
package org.eclipse.equinox.internal.p2.ui.admin;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RemoveRepositoryJob;

/**
 * Operation that removes the metadata repository with the given location.
 * 
 * @since 3.4
 */
public class RemoveMetadataRepositoryOperation extends RemoveRepositoryJob {

	public RemoveMetadataRepositoryOperation(String label, ProvisioningSession session, URI[] repoLocations) {
		super(label, session, repoLocations);
	}

	protected IStatus doSignaledWork(IProgressMonitor monitor) {
		SubMonitor mon = SubMonitor.convert(monitor, locations.length);
		for (int i = 0; i < locations.length; i++) {
			getSession().getMetadataRepositoryManager().removeRepository(locations[i]);
			mon.worked(1);
		}
		return Status.OK_STATUS;
	}
}
