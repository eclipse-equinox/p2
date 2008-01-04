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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIMessages;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.p2.ui.admin.ProvAdminUIActivator;
import org.eclipse.equinox.p2.ui.operations.AddProfileOperation;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.*;

/**
 * Dialog that allows a profile to be defined and added.
 * 
 * @since 3.4
 * 
 */
public class AddProfileDialog extends StatusDialog {

	private ProfileGroup profileGroup;
	private Button okButton;
	private String[] knownProfileIds;
	private String addedProfileId;

	public AddProfileDialog(Shell parentShell, String[] knownProfiles) {

		super(parentShell);
		this.knownProfileIds = knownProfiles;
		setTitle(ProvAdminUIMessages.AddProfileDialog_Title);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	protected Control createDialogArea(Composite parent) {
		profileGroup = new ProfileGroup(parent, null, new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				verifyComplete();
			}
		});
		Dialog.applyDialogFont(profileGroup.getComposite());
		return profileGroup.getComposite();
	}

	protected void okPressed() {
		addProfile();
		super.okPressed();
	}

	/*
	 * We only get here if already validated (ok was pressed)
	 */
	private void addProfile() {
		profileGroup.updateProfile();
		Profile profile = profileGroup.getProfile();
		if (profile == null) {
			return;
		}
		addedProfileId = profile.getProfileId();
		ProvisioningOperationRunner.schedule(new AddProfileOperation(ProvAdminUIMessages.AddProfileDialog_OperationLabel, profile), getShell());
	}

	void verifyComplete() {
		if (okButton == null) {
			return;
		}
		IStatus status = profileGroup.verify();
		if (!status.isOK()) {
			this.updateStatus(status);
			setOkEnablement(false);
			return;
		}
		if (isDuplicate()) {
			return;
		}
		okButton.setEnabled(true);
		this.updateStatus(new Status(IStatus.OK, ProvAdminUIActivator.PLUGIN_ID, IStatus.OK, "", null)); //$NON-NLS-1$

	}

	private boolean isDuplicate() {
		if (knownProfileIds == null) {
			return false;
		}

		for (int i = 0; i < knownProfileIds.length; i++) {
			if (knownProfileIds[i].equals(profileGroup.getProfileId())) {
				setOkEnablement(false);
				this.updateStatus(new Status(IStatus.ERROR, ProvAdminUIActivator.PLUGIN_ID, IStatus.OK, ProvAdminUIMessages.AddProfileDialog_DuplicateProfileID, null));
				return true;
			}
		}
		return false;
	}

	protected void updateButtonsEnableState(IStatus status) {
		setOkEnablement(!status.matches(IStatus.ERROR));
	}

	protected void setOkEnablement(boolean enable) {
		if (okButton != null && !okButton.isDisposed())
			okButton.setEnabled(enable);
	}

	/**
	 * Return the profile id that was added with this dialog, or null
	 * if no profile has been added.  This method will not return
	 * a valid profile until the user has pressed OK and the profile
	 * has been added to the profile registry.
	 * @return the added profile's id
	 */
	public String getAddedProfileId() {
		return addedProfileId;
	}
}
