/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.io.File;
import java.net.URL;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.sdk.externalFiles.ExternalFileHandler;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.AddRepositoryDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Dialog that allows colocated metadata and artifact repositories
 * to be defined and added.
 * 
 * @since 3.4
 * 
 */
public class AddColocatedRepositoryDialog extends AddRepositoryDialog {

	public AddColocatedRepositoryDialog(Shell parentShell, URL[] knownRepositories) {
		super(parentShell, knownRepositories);

	}

	protected ProvisioningOperation getOperation(URL url) {
		return new AddColocatedRepositoryOperation(getShell().getText(), url);
	}

	protected IStatus handleInvalidRepositoryURL(URL url, final IStatus status) {
		// If it was set up with jar protocol, now convert it back to file.

		if (!FILE_PROTOCOL.equalsIgnoreCase(url.getProtocol()))
			return status;
		String path = url.getPath();
		if (path.startsWith(JAR_PATH_PREFIX))
			path = path.substring(JAR_PATH_PREFIX.length());
		if (path.endsWith(JAR_PATH_SUFFIX))
			path = path.substring(0, path.length() - JAR_PATH_SUFFIX.length());
		final File file = new File(path);

		IStatus externalFileStatus = new ExternalFileHandler(getProfile(), file, getShell()).processFile(status);
		if (externalFileStatus.getCode() == ExternalFileHandler.REPO_GENERATED || externalFileStatus.getCode() == ExternalFileHandler.BUNDLE_INSTALLED) {
			// TODO workaround for bug #199806
			ProvisioningUtil.notifyRepositoryAdded();
			return Status.CANCEL_STATUS;
		}
		return externalFileStatus;
	}

	private IProfile getProfile() {
		try {
			return ProvisioningUtil.getProfile(ProvSDKUIActivator.getProfileId());
		} catch (ProvisionException e) {
			ProvUI.handleException(e, ProvSDKMessages.AddColocatedRepositoryDialog_MissingProfile, StatusManager.LOG);
			return null;
		}
	}
}
