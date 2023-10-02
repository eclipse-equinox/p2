/*******************************************************************************
 *  Copyright (c) 2023 Erik Brangs.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Erik Brangs - initial implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.net.URI;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

public class MultipleLocationsNotFoundDialog implements Runnable {

	private RepositoryTracker repositoryTracker;
	private ProvisioningUI ui;
	private URI[] locations;

	public MultipleLocationsNotFoundDialog(RepositoryTracker repositoryTracker, ProvisioningUI ui, URI[] locations) {
		this.repositoryTracker = repositoryTracker;
		this.ui = ui;
		this.locations = locations;
	}

	@Override
	public void run() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench.isClosing())
			return;
		Shell shell = ProvUI.getDefaultParentShell();
		int result = MessageDialog.open(MessageDialog.QUESTION, shell,
				ProvUIMessages.ColocatedRepositoryTracker_MultipleSitesNotFoundTitle,
				ProvUIMessages.ColocatedRepositoryTracker_PromptForMultipleSites, SWT.NONE,
				ProvUIMessages.ColocatedRepositoryTracker_MultipleSitesNotFound_RemoveButtonLabel,
				IDialogConstants.NO_LABEL,
				ProvUIMessages.ColocatedRepositoryTracker_MultipleSitesNotFound_DisableButtonLabel);
		boolean disableRepositories = result == 2;
		boolean removeRepositories = result == 0;
		if (disableRepositories) {
			disableRepositories();
		} else if (removeRepositories) {
			removeRepositories();
		}
	}

	public void removeRepositories() {
		repositoryTracker.removeRepositories(locations, ui.getSession());
	}

	public void disableRepositories() {
		for (URI location : locations) {
			disableRepository(location);
		}
	}

	private void disableRepository(URI location) {
		ui.signalRepositoryOperationStart();
		try {
			getArtifactRepositoryManager().setEnabled(location, false);
		} finally {
			RepositoryEvent artifactRepositoryDisabled = new RepositoryEvent(location, IRepository.TYPE_ARTIFACT,
					RepositoryEvent.ENABLEMENT, false);
			ui.signalRepositoryOperationComplete(artifactRepositoryDisabled, true);
		}
		ui.signalRepositoryOperationStart();
		try {
			getMetadataRepositoryManager().setEnabled(location, false);
		} finally {
			RepositoryEvent metadataRepositoryDisabled = new RepositoryEvent(location, IRepository.TYPE_METADATA,
					RepositoryEvent.ENABLEMENT, false);
			ui.signalRepositoryOperationComplete(metadataRepositoryDisabled, true);
		}
	}

	private IMetadataRepositoryManager getMetadataRepositoryManager() {
		return ProvUI.getMetadataRepositoryManager(ui.getSession());
	}

	private IArtifactRepositoryManager getArtifactRepositoryManager() {
		return ProvUI.getArtifactRepositoryManager(ui.getSession());
	}

}
