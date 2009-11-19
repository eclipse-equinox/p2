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
package org.eclipse.equinox.internal.p2.ui;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RemoveRepositoryJob;

/**
 * Operation that removes the colocated repositories with the given locations. *
 * 
 * @since 3.6
 */
public class RemoveColocatedRepositoryJob extends RemoveRepositoryJob {

	public RemoveColocatedRepositoryJob(String label, ProvisioningSession session, URI[] repoLocations) {
		super(label, session, repoLocations);
	}

	protected IStatus doSignaledWork(IProgressMonitor monitor) {
		SubMonitor mon = SubMonitor.convert(monitor, locations.length * 2);

		for (int i = 0; i < locations.length; i++) {
			getSession().getMetadataRepositoryManager().removeRepository(locations[i]);
			mon.worked(1);
			getSession().getArtifactRepositoryManager().removeRepository(locations[i]);
			mon.worked(1);
		}
		return Status.OK_STATUS;
	}
}
