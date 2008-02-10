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

import java.net.URL;
import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIMessages;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.AddRepositoryDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.AddMetadataRepositoryOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog that allows a metadata repository to be defined and added.
 * 
 * @since 3.4
 * 
 */
public class AddMetadataRepositoryDialog extends AddRepositoryDialog {

	public AddMetadataRepositoryDialog(Shell parentShell, URL[] knownRepositories) {
		super(parentShell, knownRepositories);
	}

	protected ProvisioningOperation getOperation(URL url) {
		return new AddMetadataRepositoryOperation(ProvAdminUIMessages.AddMetadataRepositoryDialog_OperationLabel, url);
	}

	protected URL makeRepositoryURL(URL newURL) {
		// TODO need to do better validation of the URL
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=211102	
		// For now we don't further process the URL
		return newURL;
	}
}
