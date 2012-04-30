/*******************************************************************************
 *  Copyright (c) 2008, 2011 IBM Corporation and others.
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
import org.eclipse.equinox.internal.p2.ui.dialogs.RepositoryNameAndLocationDialog;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Provides a repository tracker that interprets URLs as colocated
 * artifact and metadata repositories.  
 * 
 * @since 2.0
 */

public class ColocatedRepositoryTracker extends RepositoryTracker {

	ProvisioningUI ui;
	String parsedNickname;
	URI parsedLocation;

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
		return getMetadataRepositoryManager().getKnownRepositories(getMetadataRepositoryFlags());
	}

	public void addRepository(URI repoLocation, String nickname, ProvisioningSession session) {
		ui.signalRepositoryOperationStart();
		try {
			getMetadataRepositoryManager().addRepository(repoLocation);
			getArtifactRepositoryManager().addRepository(repoLocation);
			getMetadataRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_SYSTEM, Boolean.FALSE.toString());
			getArtifactRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_SYSTEM, Boolean.FALSE.toString());
			if (nickname != null) {
				getMetadataRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_NICKNAME, nickname);
				getArtifactRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_NICKNAME, nickname);

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
				getMetadataRepositoryManager().removeRepository(repoLocations[i]);
				getArtifactRepositoryManager().removeRepository(repoLocations[i]);
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
				getArtifactRepositoryManager().refreshRepository(locations[i], mon.newChild(50));
				getMetadataRepositoryManager().refreshRepository(locations[i], mon.newChild(50));
			} catch (ProvisionException e) {
				//ignore problematic repositories when refreshing
			}
		}
		// We have no idea how many repos may have been added/removed as a result of 
		// refreshing these, this one, so we do not use a specific repository event to represent it.
		ui.signalRepositoryOperationComplete(null, true);
	}

	public void reportLoadFailure(final URI location, ProvisionException e) {
		int code = e.getStatus().getCode();
		// If the user doesn't have a way to manage repositories, then don't report failures.
		if (!ui.getPolicy().getRepositoriesVisible()) {
			super.reportLoadFailure(location, e);
			return;
		}

		// Special handling when the location is bad (not found, etc.) vs. a failure
		// associated with a known repo.
		if (code == ProvisionException.REPOSITORY_NOT_FOUND || code == ProvisionException.REPOSITORY_INVALID_LOCATION) {
			if (!hasNotFoundStatusBeenReported(location)) {
				addNotFound(location);
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						IWorkbench workbench = PlatformUI.getWorkbench();
						if (workbench.isClosing())
							return;
						Shell shell = ProvUI.getDefaultParentShell();
						if (MessageDialog.openQuestion(shell, ProvUIMessages.ColocatedRepositoryTracker_SiteNotFoundTitle, NLS.bind(ProvUIMessages.ColocatedRepositoryTracker_PromptForSiteLocationEdit, URIUtil.toUnencodedString(location)))) {
							RepositoryNameAndLocationDialog dialog = new RepositoryNameAndLocationDialog(shell, ui) {
								protected String getInitialLocationText() {
									return URIUtil.toUnencodedString(location);
								}

								protected String getInitialNameText() {
									String nickname = getMetadataRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NICKNAME);
									return nickname == null ? "" : nickname; //$NON-NLS-1$
								}
							};
							int ret = dialog.open();
							if (ret == Window.OK) {
								URI correctedLocation = dialog.getLocation();
								if (correctedLocation != null) {
									ui.signalRepositoryOperationStart();
									try {
										removeRepositories(new URI[] {location}, ui.getSession());
										addRepository(correctedLocation, dialog.getName(), ui.getSession());
									} finally {
										ui.signalRepositoryOperationComplete(null, true);
									}
								}
							}
						}
					}
				});
			}
		} else {
			ProvUI.handleException(e, null, StatusManager.SHOW | StatusManager.LOG);
		}
	}

	IMetadataRepositoryManager getMetadataRepositoryManager() {
		return ProvUI.getMetadataRepositoryManager(ui.getSession());
	}

	IArtifactRepositoryManager getArtifactRepositoryManager() {
		return ProvUI.getArtifactRepositoryManager(ui.getSession());
	}

	/*
	 * Overridden to support "Name - Location" parsing
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.RepositoryTracker#locationFromString(java.lang.String)
	 */
	public URI locationFromString(String locationString) {
		URI uri = super.locationFromString(locationString);
		if (uri != null)
			return uri;
		// Look for the "Name - Location" pattern
		// There could be a hyphen in the name or URI, so we have to visit all combinations
		int start = 0;
		int index = 0;
		String locationSubset;
		String pattern = ProvUIMessages.RepositorySelectionGroup_NameAndLocationSeparator;
		while (index >= 0) {
			index = locationString.indexOf(pattern, start);
			if (index >= 0) {
				start = index + pattern.length();
				locationSubset = locationString.substring(start);
				uri = super.locationFromString(locationSubset);
				if (uri != null) {
					parsedLocation = uri;
					parsedNickname = locationString.substring(0, index);
					return uri;
				}
			}
		}
		return null;
	}

	/*
	 * Used by the UI to get a name that might have been supplied when the
	 * location was originally parsed.
	 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=293068
	 */
	public String getParsedNickname(URI location) {
		if (parsedNickname == null || parsedLocation == null)
			return null;
		if (location.toString().equals(parsedLocation.toString()))
			return parsedNickname;
		return null;
	}

	@Override
	protected boolean contains(URI location, ProvisioningSession session) {
		return ProvUI.getMetadataRepositoryManager(session).contains(location);
	}
}
