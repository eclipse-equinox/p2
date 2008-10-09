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

import java.net.URI;
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

	public AddMetadataRepositoryDialog(Shell parentShell, int repoFlags) {
		super(parentShell, repoFlags);
	}

	protected ProvisioningOperation getOperation(URI location) {
		return new AddMetadataRepositoryOperation(ProvAdminUIMessages.AddMetadataRepositoryDialog_OperationLabel, location);
	}
}
