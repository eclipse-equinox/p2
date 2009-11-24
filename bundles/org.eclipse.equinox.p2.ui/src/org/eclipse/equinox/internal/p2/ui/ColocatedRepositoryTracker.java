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
import org.eclipse.equinox.internal.provisional.p2.repository.*;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;

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

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getKnownRepositories()
	 */
	public URI[] getKnownRepositories(ProvisioningSession session) {
		return session.getMetadataRepositoryManager().getKnownRepositories(getMetadataRepositoryFlags());
	}

	protected IStatus validateRepositoryLocationWithManager(ProvisioningSession session, URI location, IProgressMonitor monitor) {
		return session.getMetadataRepositoryManager().validateRepositoryLocation(location, monitor);
	}

	public void addRepository(URI repoLocation, String nickname, ProvisioningSession session) {
		session.signalOperationStart();
		try {
			session.getMetadataRepositoryManager().addRepository(repoLocation);
			session.getArtifactRepositoryManager().addRepository(repoLocation);
			if (nickname != null) {
				session.getMetadataRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_NICKNAME, nickname);
				session.getArtifactRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_NICKNAME, nickname);

			}
		} finally {
			// We know that the UI only responds to metadata repo events so we cheat...
			session.signalOperationComplete(new RepositoryEvent(repoLocation, IRepository.TYPE_METADATA, RepositoryEvent.ADDED, true), false);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.RepositoryTracker#removeRepositories(java.net.URI[], org.eclipse.equinox.p2.operations.ProvisioningSession)
	 */
	public void removeRepositories(URI[] repoLocations, ProvisioningSession session) {
		session.signalOperationStart();
		try {
			for (int i = 0; i < repoLocations.length; i++) {
				session.getMetadataRepositoryManager().removeRepository(repoLocations[i]);
				session.getArtifactRepositoryManager().removeRepository(repoLocations[i]);
			}
		} finally {
			session.signalOperationComplete(null, false);
		}
	}
}
