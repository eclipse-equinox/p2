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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * UpdateHandler invokes the main update/install UI.
 * 
 * @since 3.4
 */
public class UpdateHandler extends AbstractHandler {

	/**
	 * The constructor.
	 */
	public UpdateHandler() {
		// constructor
	}

	/**
	 * Execute the update command.
	 */
	public Object execute(ExecutionEvent event) {
		String profileId;

		// Need to figure out the profile we are using and open a dialog
		try {
			profileId = ProvSDKUIActivator.getSelfProfileId();
		} catch (ProvisionException e) {
			profileId = null;
		}
		if (profileId != null) {
			openDialog(null, profileId);
		} else {
			MessageDialog.openInformation(null, ProvSDKMessages.UpdateHandler_SDKUpdateUIMessageTitle, ProvSDKMessages.UpdateHandler_CannotLaunchUI);
		}
		return null;
	}

	protected void openDialog(Shell shell, String profileId) {
		UpdateAndInstallDialog dialog = new UpdateAndInstallDialog(shell, profileId);
		dialog.open();
	}
}
