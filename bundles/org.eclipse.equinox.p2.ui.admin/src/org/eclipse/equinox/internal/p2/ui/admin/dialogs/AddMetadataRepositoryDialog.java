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
import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIMessages;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.dialogs.AddRepositoryDialog;
import org.eclipse.equinox.p2.ui.operations.AddMetadataRepositoryOperation;
import org.eclipse.equinox.p2.ui.operations.ProvisioningOperation;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog that allows a metadata repository to be defined and added.
 * 
 * @since 3.4
 * 
 */
public class AddMetadataRepositoryDialog extends AddRepositoryDialog {

	public AddMetadataRepositoryDialog(Shell parentShell, IMetadataRepository[] knownRepositories) {
		super(parentShell, knownRepositories);
	}

	protected ProvisioningOperation getOperation(URL url, String name) {
		return new AddMetadataRepositoryOperation(ProvAdminUIMessages.AddMetadataRepositoryDialog_OperationLabel, url, name);
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
