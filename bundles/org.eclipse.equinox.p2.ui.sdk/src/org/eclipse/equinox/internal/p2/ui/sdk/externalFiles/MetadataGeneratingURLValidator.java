/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.sdk.externalFiles;

import java.io.File;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.DefaultURLValidator;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.URLValidator;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * @since 3.4
 *
 */
public class MetadataGeneratingURLValidator extends DefaultURLValidator {

	Shell shell;
	IProfile profile;

	public void setShell(Shell shell) {
		this.shell = shell;
	}

	public void setProfile(IProfile profile) {
		this.profile = profile;
	}

	protected IStatus validateRepositoryURL(URL location, boolean contactRepositories, IProgressMonitor monitor) {
		IStatus status = super.validateRepositoryURL(location, contactRepositories, monitor);

		// If it's already OK or the problem was a local format, nothing to do here.
		if (status.isOK() || status.getCode() == URLValidator.LOCAL_VALIDATION_ERROR)
			return status;

		if (shell == null)
			shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		if (profile == null) {
			try {
				profile = ProvisioningUtil.getProfile(IProfileRegistry.SELF);
			} catch (ProvisionException e) {
				return status;
			}

		}

		// If it was set up with jar protocol, now convert it back to file.
		if (!FILE_PROTOCOL.equalsIgnoreCase(location.getProtocol()))
			return status;
		String path = location.getPath();
		if (path.startsWith(JAR_PATH_PREFIX))
			path = path.substring(JAR_PATH_PREFIX.length());
		if (path.endsWith(JAR_PATH_SUFFIX))
			path = path.substring(0, path.length() - JAR_PATH_SUFFIX.length());
		final File file = new File(path);

		IStatus externalFileStatus = new ExternalFileHandler(profile, file, shell).processFile(status);
		if (externalFileStatus.getCode() == REPO_AUTO_GENERATED || externalFileStatus.getCode() == ALTERNATE_ACTION_TAKEN) {
			// TODO workaround for bug #199806
			ProvisioningUtil.notifyRepositoryAdded();
			return Status.CANCEL_STATUS;
		}
		return externalFileStatus;
	}

}
