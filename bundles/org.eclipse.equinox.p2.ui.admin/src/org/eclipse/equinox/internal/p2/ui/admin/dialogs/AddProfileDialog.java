/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin.dialogs;

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.admin.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.StatusDialog;
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

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		profileGroup = new ProfileGroup(parent, null, event -> verifyComplete());
		Dialog.applyDialogFont(profileGroup.getComposite());
		return profileGroup.getComposite();
	}

	@Override
	protected void okPressed() {
		verifyComplete();
		if (okButton.isEnabled()) {
			addProfile();
			super.okPressed();
		}
	}

	/*
	 * We only get here if already validated (ok was pressed)
	 */
	private void addProfile() {
		IProfile profile = profileGroup.getProfile();
		if (profile != null) {
			return;
		}
		addedProfileId = profileGroup.getProfileId();
		Map<String, String> profileProperties = profileGroup.getProfileProperties();
		AddProfileJob job = new AddProfileJob(ProvAdminUIMessages.AddProfileDialog_OperationLabel, ProvisioningUI.getDefaultUI().getSession(), addedProfileId, profileProperties);
		job.runModal(new NullProgressMonitor());
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

	@Override
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
