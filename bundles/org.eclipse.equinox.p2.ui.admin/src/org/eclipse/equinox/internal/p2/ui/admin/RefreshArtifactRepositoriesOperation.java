/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
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
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;

/**
 * @since 3.4
 *
 */
public class RefreshArtifactRepositoriesOperation extends ProvisioningJob {

	private URI[] locations;

	/**
	 * @param label
	 * @param locations
	 */
	public RefreshArtifactRepositoriesOperation(String label, ProvisioningSession session, URI[] locations) {
		super(label, session);
		this.locations = locations;
	}

	public RefreshArtifactRepositoriesOperation(String label, ProvisioningSession session, int flags) {
		super(label, session);
		this.locations = session.getArtifactRepositoryManager().getKnownRepositories(flags);
	}

	public IStatus runModal(IProgressMonitor monitor) {
		getSession().refreshArtifactRepositories(locations, monitor);
		return Status.OK_STATUS;
	}
}
