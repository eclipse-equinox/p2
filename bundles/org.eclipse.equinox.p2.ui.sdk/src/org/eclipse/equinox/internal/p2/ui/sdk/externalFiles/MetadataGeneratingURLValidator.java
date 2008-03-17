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
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.DefaultMetadataURLValidator;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.URLValidator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * @since 3.4
 *
 */
public class MetadataGeneratingURLValidator extends DefaultMetadataURLValidator {

	Shell shell;

	public void setShell(Shell shell) {
		this.shell = shell;
	}

	protected IStatus validateRepositoryURL(URL location, boolean contactRepositories, IProgressMonitor monitor) {
		IStatus status = super.validateRepositoryURL(location, contactRepositories, monitor);

		// If it's already OK or the problem was a local format, nothing to do here.
		if (status.isOK() || status.getCode() == URLValidator.LOCAL_VALIDATION_ERROR)
			return status;

		if (shell == null)
			shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		// If it was set up with jar protocol, now convert it back to file.
		String path = location.toExternalForm();
		if (path.startsWith(JAR_PATH_PREFIX))
			path = path.substring(JAR_PATH_PREFIX.length());
		if (path.endsWith(JAR_PATH_SUFFIX))
			path = path.substring(0, path.length() - JAR_PATH_SUFFIX.length());
		if (!path.startsWith(FILE_PROTOCOL_PREFIX))
			return status;
		final File file = new File(path.substring(FILE_PROTOCOL_PREFIX.length()));

		IStatus externalFileStatus = new ExternalFileHandler(file, shell).processFile(status);
		if (externalFileStatus.getCode() == REPO_AUTO_GENERATED || externalFileStatus.getCode() == ALTERNATE_ACTION_TAKEN) {
			return Status.CANCEL_STATUS;
		}
		return externalFileStatus;
	}

}
