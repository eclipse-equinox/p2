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
package org.eclipse.equinox.prov.ui.internal.sdk;

import org.eclipse.core.commands.*;
import org.eclipse.equinox.prov.core.ProvisionException;
import org.eclipse.equinox.prov.engine.IProfileRegistry;
import org.eclipse.equinox.prov.engine.Profile;
import org.eclipse.equinox.prov.ui.ProvisioningUtil;
import org.eclipse.equinox.prov.ui.model.AllProfiles;
import org.eclipse.equinox.prov.ui.model.ProfileFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * UpdateHandler invokes the new provisioning update UI.
 * 
 * @since 3.4
 */
public class UpdateHandler extends AbstractHandler {

	private static final String DEFAULT_PROFILE_ID = "DefaultProfile"; //$NON-NLS-1$

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
		Shell shell = HandlerUtil.getActiveWorkbenchWindowChecked(event).getShell();
		Profile profile = null;
		String message = null;
		// Get the profile of the running system.
		try {
			profile = ProvisioningUtil.getProfile(IProfileRegistry.SELF);
		} catch (ProvisionException e) {
			profile = null;
			message = ProvSDKMessages.UpdateHandler_NoProfilesDefined;
		}
		if (profile == null) {
			profile = getAnyProfile();
		}

		if (profile != null) {
			UpdateAndInstallDialog dialog = new UpdateAndInstallDialog(shell, profile);
			dialog.open();
		} else {
			if (message == null)
				message = ProvSDKMessages.UpdateHandler_NoProfileInstanceDefined;
			MessageDialog.openInformation(shell, ProvSDKMessages.UpdateHandler_SDKUpdateUIMessageTitle, message);
		}
		return null;
	}

	// TODO this is temporary so the UI will come up on something
	private Profile getAnyProfile() {
		Profile[] profiles = (Profile[]) new AllProfiles().getChildren(null);
		if (profiles.length > 0)
			return profiles[0];
		return ProfileFactory.makeProfile(DEFAULT_PROFILE_ID);
	}
}
