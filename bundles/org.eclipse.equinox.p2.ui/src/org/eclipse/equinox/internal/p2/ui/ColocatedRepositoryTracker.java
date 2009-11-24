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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.repository.*;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * Provides a repository tracker that interprets URLs as colocated
 * artifact and metadata repositories.  
 * 
 * @since 2.0
 */

public class ColocatedRepositoryTracker extends RepositoryTracker {

	ProvisioningUI ui;

	public ColocatedRepositoryTracker(ProvisioningUI ui) {
		this.ui = ui;
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
		ui.signalRepositoryOperationStart();
		try {
			session.getMetadataRepositoryManager().addRepository(repoLocation);
			session.getArtifactRepositoryManager().addRepository(repoLocation);
			if (nickname != null) {
				session.getMetadataRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_NICKNAME, nickname);
				session.getArtifactRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_NICKNAME, nickname);

			}
		} finally {
			// We know that the UI only responds to metadata repo events so we cheat...
			ui.signalRepositoryOperationComplete(new RepositoryEvent(repoLocation, IRepository.TYPE_METADATA, RepositoryEvent.ADDED, true), true);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.RepositoryTracker#removeRepositories(java.net.URI[], org.eclipse.equinox.p2.operations.ProvisioningSession)
	 */
	public void removeRepositories(URI[] repoLocations, ProvisioningSession session) {
		ui.signalRepositoryOperationStart();
		try {
			for (int i = 0; i < repoLocations.length; i++) {
				session.getMetadataRepositoryManager().removeRepository(repoLocations[i]);
				session.getArtifactRepositoryManager().removeRepository(repoLocations[i]);
			}
		} finally {
			ui.signalRepositoryOperationComplete(null, true);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.RepositoryTracker#refreshRepositories(java.net.URI[], org.eclipse.equinox.p2.operations.ProvisioningSession, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void refreshRepositories(URI[] locations, ProvisioningSession session, IProgressMonitor monitor) {
		ui.signalRepositoryOperationStart();
		SubMonitor mon = SubMonitor.convert(monitor, locations.length * 100);
		for (int i = 0; i < locations.length; i++) {
			try {
				session.getArtifactRepositoryManager().refreshRepository(locations[i], mon.newChild(50));
				session.getMetadataRepositoryManager().refreshRepository(locations[i], mon.newChild(50));
			} catch (ProvisionException e) {
				//ignore problematic repositories when refreshing
			}
		}
		// We have no idea how many repos may have been added/removed as a result of 
		// refreshing these, this one, so we do not use a specific repository event to represent it.
		ui.signalRepositoryOperationComplete(null, true);
	}
}
