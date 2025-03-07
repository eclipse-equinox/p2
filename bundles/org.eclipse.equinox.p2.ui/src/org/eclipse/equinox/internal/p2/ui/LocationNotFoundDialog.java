/*******************************************************************************
 *  Copyright (c) 2008, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.net.URI;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.ui.dialogs.RepositoryNameAndLocationDialog;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

public class LocationNotFoundDialog implements Runnable {

	private final RepositoryTracker repositoryTracker;
	private final ProvisioningUI ui;
	private final URI location;

	public LocationNotFoundDialog(RepositoryTracker repositoryTracker, ProvisioningUI ui, URI location) {
		this.repositoryTracker = repositoryTracker;
		this.ui = ui;
		this.location = location;
	}

	@Override
	public void run() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench.isClosing()) {
			return;
		}
		Shell shell = ProvUI.getDefaultParentShell();
		int result = MessageDialog.open(MessageDialog.QUESTION, shell,
				ProvUIMessages.ColocatedRepositoryTracker_SiteNotFoundTitle,
				NLS.bind(ProvUIMessages.ColocatedRepositoryTracker_PromptForSiteLocationEdit,
						URIUtil.toUnencodedString(location)),
				SWT.NONE, ProvUIMessages.ColocatedRepositoryTracker_SiteNotFound_RemoveButtonLabel,
				IDialogConstants.NO_LABEL, ProvUIMessages.ColocatedRepositoryTracker_SiteNotFound_EditButtonLabel,
				ProvUIMessages.ColocatedRepositoryTracker_SiteNotFound_DisableButtonLabel);
		boolean editRepository = result == 2;
		boolean disableRepository = result == 3;
		boolean removeRepository = result == 0;
		if (editRepository) {
			RepositoryNameAndLocationDialog dialog = new RepositoryNameAndLocationDialog(shell, ui) {
				@Override
				protected String getInitialLocationText() {
					return URIUtil.toUnencodedString(location);
				}

				@Override
				protected String getInitialNameText() {
					String nickname = getMetadataRepositoryManager().getRepositoryProperty(location,
							IRepository.PROP_NICKNAME);
					return nickname == null ? "" : nickname; //$NON-NLS-1$
				}
			};
			int ret = dialog.open();
			if (ret == Window.OK) {
				URI correctedLocation = dialog.getLocation();
				if (correctedLocation != null) {
					String name = dialog.getName();
					correctLocation(correctedLocation, name);
				}
			}
		} else if (disableRepository) {
			disableRepository();
		} else if (removeRepository) {
			removeRepository();
		}
	}

	public void removeRepository() {
		repositoryTracker.removeRepositories(new URI[] { location }, ui.getSession());
	}

	public void disableRepository() {
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

	public void correctLocation(URI correctedLocation, String repositoryName) {
		ui.signalRepositoryOperationStart();
		try {
			repositoryTracker.removeRepositories(new URI[] { location }, ui.getSession());
			repositoryTracker.addRepository(correctedLocation, repositoryName, ui.getSession());
		} finally {
			ui.signalRepositoryOperationComplete(null, true);
		}
	}

	private IMetadataRepositoryManager getMetadataRepositoryManager() {
		return ProvUI.getMetadataRepositoryManager(ui.getSession());
	}

	private IArtifactRepositoryManager getArtifactRepositoryManager() {
		return ProvUI.getArtifactRepositoryManager(ui.getSession());
	}

}
