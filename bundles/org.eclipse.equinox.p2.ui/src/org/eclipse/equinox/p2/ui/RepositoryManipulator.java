/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui;

import java.net.URI;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.RepositoryNameAndLocationDialog;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Abstract class for a mechanism that describes how repositories are manipulated
 * by the user, and how errors are reported to the user.
 * 
 * @since 2.0
 * 
 */

public abstract class RepositoryManipulator extends RepositoryTracker {

	private boolean visible = true;

	/**
	 * Invoke whatever mechanism is used to manipulate repositories.
	 * Return a boolean indicating whether the repositories were
	 * actually manipulated in any way.
	 */
	public abstract boolean manipulateRepositories(Shell shell, ProvisioningUI ui);

	public void reportLoadFailure(final URI location, IStatus status, final ProvisioningUI ui) {
		if (!getRepositoriesVisible()) {
			ProvUI.reportStatus(status, StatusManager.LOG);
			return;
		}
		int code = status.getCode();
		// Special handling when the location is bad (not found, etc.) vs. a failure
		// associated with a known repo.
		if (code == ProvisionException.REPOSITORY_NOT_FOUND || code == ProvisionException.REPOSITORY_INVALID_LOCATION) {
			if (!hasNotFoundStatusBeenReported(location)) {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						IWorkbench workbench = PlatformUI.getWorkbench();
						if (workbench.isClosing())
							return;
						Shell shell = ProvUI.getDefaultParentShell();
						if (MessageDialog.openQuestion(shell, ProvUIMessages.ProvUI_LoadErrorTitle, NLS.bind(ProvUIMessages.ProvUI_PromptForSiteEdit, URIUtil.toUnencodedString(location)))) {
							RepositoryNameAndLocationDialog dialog = new RepositoryNameAndLocationDialog(shell, ui) {
								protected String getInitialLocationText() {
									return URIUtil.toUnencodedString(location);
								}

								protected String getInitialNameText() {
									String nickname = ui.getSession().getMetadataRepositoryProperty(location, IRepository.PROP_NICKNAME);
									return nickname == null ? "" : nickname; //$NON-NLS-1$
								}
							};
							int ret = dialog.open();
							if (ret == Window.OK) {
								URI correctedLocation = dialog.getLocation();
								if (correctedLocation != null) {
									ui.getSession().signalBatchOperationStart();
									ProvisioningJob op = getRemoveOperation(new URI[] {location}, ui);
									op.runModal(null);
									ui.getSession().signalBatchOperationComplete(false, location);
									op = getAddOperation(correctedLocation, ui);
									IStatus addStatus = op.runModal(null);
									if (addStatus.getSeverity() == IStatus.ERROR) {
										ProvUI.reportStatus(addStatus, StatusManager.SHOW | StatusManager.LOG);
										ui.getSession().signalBatchOperationComplete(true, null);
									} else {
										String nickname = dialog.getName();
										if (nickname != null && nickname.length() > 0)
											ui.getSession().setMetadataRepositoryProperty(correctedLocation, IRepository.PROP_NICKNAME, nickname);
										ui.getSession().signalBatchOperationComplete(true, correctedLocation);
									}
								}
							}
						}
					}
				});
				addNotFound(location);
			}
		} else {
			ProvUI.reportStatus(status, StatusManager.SHOW);
		}
	}

	/**
	 * Return an operation that could be used to add the specified URI as
	 * a repository.
	 */
	public abstract AddRepositoryJob getAddOperation(URI repoLocation, ProvisioningUI ui);

	/**
	 * Return an operation that could be used to remove the specified URL as
	 * a repositories.
	 */
	public abstract RemoveRepositoryJob getRemoveOperation(URI[] repoLocations, ProvisioningUI ui);

	public boolean getRepositoriesVisible() {
		return visible;
	}

	public void setRepositoriesVisible(boolean visible) {
		this.visible = visible;
	}

	public String getManipulatorLinkLabel() {
		return ProvUIMessages.ColocatedRepositoryManipulator_GotoPrefs;

	}
}
