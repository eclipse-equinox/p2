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

import java.net.URL;
import org.eclipse.equinox.internal.p2.ui.sdk.externalFiles.MetadataGeneratingURLValidator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.AddRepositoryDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.DefaultURLValidator;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Dialog that allows colocated metadata and artifact repositories
 * to be defined and added.
 * 
 * @since 3.4
 * 
 */
public class AddColocatedRepositoryDialog extends AddRepositoryDialog {

	public AddColocatedRepositoryDialog(Shell parentShell, int repoFlags) {
		super(parentShell, repoFlags);
		setTitle(ProvSDKMessages.AddColocatedRepositoryDialog_Title);

	}

	protected ProvisioningOperation getOperation(URL url) {
		return new AddColocatedRepositoryOperation(getShell().getText(), url);
	}

	protected DefaultURLValidator createURLValidator() {
		MetadataGeneratingURLValidator validator = new MetadataGeneratingURLValidator();
		validator.setProfile(getProfile());
		validator.setShell(getShell());
		return validator;
	}

	private IProfile getProfile() {
		try {
			return ProvisioningUtil.getProfile(ProvSDKUIActivator.getProfileId());
		} catch (ProvisionException e) {
			ProvUI.handleException(e, ProvSDKMessages.AddColocatedRepositoryDialog_MissingProfile, StatusManager.LOG);
			return null;
		}
	}
}
