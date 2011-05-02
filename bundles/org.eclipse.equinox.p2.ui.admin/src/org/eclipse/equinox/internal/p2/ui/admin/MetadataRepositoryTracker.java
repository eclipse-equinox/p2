/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

public class MetadataRepositoryTracker extends RepositoryTracker {

	ProvisioningUI ui;

	public MetadataRepositoryTracker(ProvisioningUI ui) {
		this.ui = ui;
	}

	public URI[] getKnownRepositories(ProvisioningSession session) {
		return getMetadataRepositoryManager().getKnownRepositories(getArtifactRepositoryFlags());
	}

	public void addRepository(URI repoLocation, String nickname, ProvisioningSession session) {
		ui.signalRepositoryOperationStart();
		try {
			getMetadataRepositoryManager().addRepository(repoLocation);
			if (nickname != null)
				getMetadataRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_NICKNAME, nickname);

		} finally {
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
				getMetadataRepositoryManager().removeRepository(repoLocations[i]);
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
				getMetadataRepositoryManager().refreshRepository(locations[i], mon.newChild(100));
			} catch (ProvisionException e) {
				//ignore problematic repositories when refreshing
			}
		}
		// We have no idea how many repos may have been added/removed as a result of 
		// refreshing these, this one, so we do not use a specific repository event to represent it.
		ui.signalRepositoryOperationComplete(null, true);
	}

	IMetadataRepositoryManager getMetadataRepositoryManager() {
		return ProvUI.getMetadataRepositoryManager(ui.getSession());
	}

	@Override
	protected boolean contains(URI location, ProvisioningSession session) {
		return ProvUI.getMetadataRepositoryManager(session).contains(location);
	}
}