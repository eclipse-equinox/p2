/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.admin;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;

public class ArtifactRepositoryTracker extends RepositoryTracker {

	public URI[] getKnownRepositories(ProvisioningSession session) {
		return session.getArtifactRepositoryManager().getKnownRepositories(getArtifactRepositoryFlags());
	}

	protected IStatus validateRepositoryLocationWithManager(ProvisioningSession session, URI location, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	public void addRepository(URI repoLocation, String nickname, ProvisioningSession session) {
		session.signalOperationStart();
		try {
			session.getArtifactRepositoryManager().addRepository(repoLocation);
			if (nickname != null)
				session.getArtifactRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_NICKNAME, nickname);
		} finally {
			session.signalOperationComplete(new RepositoryEvent(repoLocation, IRepository.TYPE_ARTIFACT, RepositoryEvent.ADDED, true), false);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.RepositoryTracker#removeRepositories(java.net.URI[], org.eclipse.equinox.p2.operations.ProvisioningSession)
	 */
	public void removeRepositories(URI[] repoLocations, ProvisioningSession session) {
		session.signalOperationStart();
		try {
			for (int i = 0; i < repoLocations.length; i++) {
				session.getArtifactRepositoryManager().removeRepository(repoLocations[i]);
			}
		} finally {
			session.signalOperationComplete(null, false);
		}
	}
}