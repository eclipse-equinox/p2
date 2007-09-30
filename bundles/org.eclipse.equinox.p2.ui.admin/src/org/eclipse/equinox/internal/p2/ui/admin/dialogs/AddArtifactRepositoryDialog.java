/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin.dialogs;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIMessages;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.dialogs.AddRepositoryDialog;
import org.eclipse.equinox.p2.ui.operations.AddArtifactRepositoryOperation;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog that allows an artifact repository to be defined and added.
 * 
 * @since 3.4
 * 
 */
public class AddArtifactRepositoryDialog extends AddRepositoryDialog {

	public AddArtifactRepositoryDialog(Shell parentShell, IArtifactRepository[] knownRepositories) {
		super(parentShell, knownRepositories);
	}

	protected IUndoableOperation getOperation(URL url, String name) {
		return new AddArtifactRepositoryOperation(ProvAdminUIMessages.AddArtifactRepositoryDialog_OperationLabel, url, name);
	}

	protected URL makeRepositoryURL(String urlString) {
		try {
			return new URL(urlString);
		} catch (MalformedURLException e) {
			// TODO need friendlier user message rather than just reporting exception
			ProvUI.handleException(e, ProvAdminUIMessages.AddRepositoryDialog_InvalidURL);
			return null;
		}
	}

	protected String repositoryFileName() {
		return null;
	}

	protected boolean repositoryIsFile() {
		return false;
	}
}
