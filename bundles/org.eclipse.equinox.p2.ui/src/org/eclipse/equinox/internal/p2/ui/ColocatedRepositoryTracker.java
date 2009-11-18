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
package org.eclipse.equinox.internal.p2.ui;

import java.net.URI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.operations.*;

/**
 * Provides a repository tracker that interprets URLs as colocated
 * artifact and metadata repositories.  
 * 
 * @since 2.0
 */

public class ColocatedRepositoryTracker extends RepositoryTracker {

	public ColocatedRepositoryTracker() {
		setArtifactRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		setMetadataRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
	}

	public AddRepositoryJob getAddOperation(URI repoLocation, ProvisioningSession session) {
		return new AddColocatedRepositoryJob(ProvUIMessages.ColocatedRepositoryManipulator_AddSiteOperationLabel, session, repoLocation);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getKnownRepositories()
	 */
	public URI[] getKnownRepositories(ProvisioningSession session) {
		return session.getMetadataRepositoryManager().getKnownRepositories(getMetadataRepositoryFlags());
	}

	public RemoveRepositoryJob getRemoveOperation(URI[] reposToRemove, ProvisioningSession session) {
		return new RemoveColocatedRepositoryJob(ProvUIMessages.ColocatedRepositoryManipulator_RemoveSiteOperationLabel, session, reposToRemove);
	}

	protected IStatus validateRepositoryLocationWithManager(ProvisioningSession session, URI location, IProgressMonitor monitor) {
		return session.getMetadataRepositoryManager().validateRepositoryLocation(location, monitor);
	}
}
