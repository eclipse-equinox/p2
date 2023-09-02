/*******************************************************************************
 * Copyright (c) 2009, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Hannes Wellmann - Unify ArtifactRepositoryTracker and MetadataRepositoryTracker to one class
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.admin;

import java.net.URI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

public class SingleRepositoryTracker extends RepositoryTracker {

	public static RepositoryTracker createMetadataRepositoryTracker(ProvisioningUI ui) {
		return new SingleRepositoryTracker(ui, IRepository.TYPE_METADATA, IMetadataRepositoryManager.class);
	}

	public static RepositoryTracker createArtifactRepositoryTracker(ProvisioningUI ui) {
		return new SingleRepositoryTracker(ui, IRepository.TYPE_ARTIFACT, IArtifactRepositoryManager.class);
	}

	private final ProvisioningUI ui;
	private final int repositoryType;
	private final Class<? extends IRepositoryManager<?>> repositoryManagerType;

	private SingleRepositoryTracker(ProvisioningUI ui, int repositoryType,
			Class<? extends IRepositoryManager<?>> repositoryManagerType) {
		this.ui = ui;
		this.repositoryType = repositoryType;
		this.repositoryManagerType = repositoryManagerType;
	}

	@Override
	public URI[] getKnownRepositories(ProvisioningSession session) {
		return getRepositoryManager().getKnownRepositories(getArtifactRepositoryFlags());
	}

	@Override
	public void addRepository(URI repoLocation, String nickname, ProvisioningSession session) {
		ui.signalRepositoryOperationStart();
		try {
			getRepositoryManager().addRepository(repoLocation);
			if (nickname != null) {
				getRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_NICKNAME, nickname);
			}
		} finally {
			ui.signalRepositoryOperationComplete(
					new RepositoryEvent(repoLocation, repositoryType, RepositoryEvent.ADDED, true), true);
		}
	}

	@Override
	public void removeRepositories(URI[] repoLocations, ProvisioningSession session) {
		ui.signalRepositoryOperationStart();
		try {
			for (URI repoLocation : repoLocations) {
				getRepositoryManager().removeRepository(repoLocation);
			}
		} finally {
			ui.signalRepositoryOperationComplete(null, true);
		}
	}

	@Override
	public void refreshRepositories(URI[] locations, ProvisioningSession session, IProgressMonitor monitor) {
		ui.signalRepositoryOperationStart();
		SubMonitor mon = SubMonitor.convert(monitor, locations.length * 100);
		for (URI location : locations) {
			try {
				getRepositoryManager().refreshRepository(location, mon.newChild(100));
			} catch (ProvisionException e) {
				// ignore problematic repositories when refreshing
			}
		}
		// We have no idea how many repos may have been added/removed as a result of
		// refreshing these, this one, so we do not use a specific repository event to
		// represent it.
		ui.signalRepositoryOperationComplete(null, true);
	}

	private IRepositoryManager<?> getRepositoryManager() {
		return ui.getSession().getProvisioningAgent().getService(repositoryManagerType);
	}

	@Override
	protected boolean contains(URI location, ProvisioningSession session) {
		return session.getProvisioningAgent().getService(repositoryManagerType).contains(location);
	}
}