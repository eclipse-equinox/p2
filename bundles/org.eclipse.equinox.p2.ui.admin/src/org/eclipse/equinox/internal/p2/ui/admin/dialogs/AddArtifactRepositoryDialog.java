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
package org.eclipse.equinox.internal.p2.ui.admin.dialogs;

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIActivator;
import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.AddRepositoryDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.DefaultURLValidator;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog that allows an artifact repository to be defined and added.
 * 
 * @since 3.4
 * 
 */
public class AddArtifactRepositoryDialog extends AddRepositoryDialog {

	public AddArtifactRepositoryDialog(Shell parentShell, int repoFlags) {
		super(parentShell, repoFlags);
	}

	protected ProvisioningOperation getOperation(URL url) {
		return new AddArtifactRepositoryOperation(ProvAdminUIMessages.AddArtifactRepositoryDialog_OperationLabel, url);
	}

	protected DefaultURLValidator createURLValidator() {
		return new DefaultURLValidator() {
			protected IStatus validateRepositoryURL(URL location, boolean contactRepositories, IProgressMonitor monitor) {
				IStatus duplicateStatus = Status.OK_STATUS;
				URL[] knownRepositories;
				try {
					knownRepositories = ProvisioningUtil.getArtifactRepositories(repoFlag);
				} catch (ProvisionException e) {
					knownRepositories = new URL[0];
				}
				for (int i = 0; i < knownRepositories.length; i++) {
					if (knownRepositories[i].toExternalForm().equalsIgnoreCase(location.toExternalForm())) {
						duplicateStatus = new Status(IStatus.ERROR, ProvAdminUIActivator.PLUGIN_ID, LOCAL_VALIDATION_ERROR, ProvAdminUIMessages.AddArtifactRepositoryDialog_DuplicateURL, null);
						break;
					}
				}
				return duplicateStatus;
			}
		};
	}
}
