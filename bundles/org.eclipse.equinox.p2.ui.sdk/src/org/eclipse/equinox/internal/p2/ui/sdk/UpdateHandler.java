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
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.core.commands.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Shell;

/**
 * UpdateHandler invokes the main update/install UI.
 * 
 * @since 3.4
 */
public class UpdateHandler extends AbstractHandler {

	UpdateAndInstallDialog dialog;

	/**
	 * The constructor.
	 */
	public UpdateHandler() {
		// constructor
	}

	/**
	 * Execute the update command.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String profileId;
		String message = null;

		// If the dialog is already open, we've already done all this.
		if (dialog != null) {
			dialog.getShell().setFocus();
			return null;
		}
		// Need to figure out the profile we are using and open a dialog
		try {
			profileId = ProvSDKUIActivator.getProfileId();
		} catch (ProvisionException e) {
			profileId = null;
			message = ProvSDKMessages.UpdateHandler_NoProfilesDefined;
		}
		if (profileId != null) {
			openDialog(null, profileId);
		} else {
			if (message == null)
				message = ProvSDKMessages.UpdateHandler_NoProfileInstanceDefined;
			MessageDialog.openInformation(null, ProvSDKMessages.UpdateHandler_SDKUpdateUIMessageTitle, message);
		}
		return null;
	}

	protected void openDialog(Shell shell, String profileId) {
		if (dialog == null) {
			dialog = new UpdateAndInstallDialog(shell, profileId);
			dialog.open();
			dialog.getShell().addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					dialog = null;
				}

			});
		} else {
			dialog.getShell().setFocus();
		}
	}
}
