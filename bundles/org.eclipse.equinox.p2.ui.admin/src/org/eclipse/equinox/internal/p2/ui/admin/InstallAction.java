/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.admin;

import java.util.Collection;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.actions.ProfileModificationAction;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddProfileDialog;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.viewers.ProvElementContentProvider;
import org.eclipse.equinox.internal.p2.ui.viewers.ProvElementLabelProvider;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.dialogs.ListDialog;

public class InstallAction extends ProfileModificationAction {

	String userChosenProfileId;

	public InstallAction(ProvisioningUI ui, ISelectionProvider selectionProvider) {
		super(ui, ProvUI.INSTALL_COMMAND_LABEL, selectionProvider, null);
		setToolTipText(ProvUI.INSTALL_COMMAND_TOOLTIP);
		userChosenProfileId = ui.getProfileId();
	}

	@Override
	protected boolean isEnabledFor(Object[] selectionArray) {
		if (selectionArray.length == 0)
			return false;
		// We allow non-IU's to be selected at this point, but there
		// must be at least one installable unit selected that is
		// selectable
		for (int i = 0; i < selectionArray.length; i++) {
			if (selectionArray[i] instanceof InstalledIUElement && isSelectable((IIUElement) selectionArray[i]))
				return true;
			IInstallableUnit iu = ProvUI.getAdapter(selectionArray[i], IInstallableUnit.class);
			if (iu != null && isSelectable(iu))
				return true;
		}
		return false;
	}

	/*
	 * Overridden to reject categories and nested IU's (parent is a non-category IU)
	 */
	@Override
	protected boolean isSelectable(IIUElement element) {
		return super.isSelectable(element) && !(element.getParent(element) instanceof AvailableIUElement);
	}

	@Override
	protected int performAction(ProfileChangeOperation operation, Collection<IInstallableUnit> ius) {
		ProvisioningUI ui = ProvAdminUIActivator.getDefault().getProvisioningUI(userChosenProfileId);
		operation.setProfileId(userChosenProfileId);
		int ret = ui.openInstallWizard(ius, (InstallOperation) operation, null);
		userChosenProfileId = null;
		return ret;
	}

	@Override
	protected ProfileChangeOperation getProfileChangeOperation(Collection<IInstallableUnit> ius) {
		InstallOperation op = new InstallOperation(getSession(), ius);
		op.setProfileId(userChosenProfileId);
		return op;
	}

	@Override
	protected boolean isInvalidProfileId() {
		if (userChosenProfileId == null) {
			userChosenProfileId = getUserChosenProfileId();
		}
		return userChosenProfileId == null;
	}

	private String getUserChosenProfileId() {
		IProfileRegistry registry = ProvAdminUIActivator.getDefault().getProfileRegistry();
		if (registry.getProfiles().length == 0) {
			AddProfileDialog dialog = new AddProfileDialog(getShell(), new String[0]);
			if (dialog.open() == Window.OK) {
				return dialog.getAddedProfileId();
			}
			return null;
		}

		ListDialog dialog = new ListDialog(getShell());
		dialog.setTitle("Choose a Profile");
		dialog.setLabelProvider(new ProvElementLabelProvider());
		dialog.setInput(new Profiles(getProvisioningUI()));
		dialog.setContentProvider(new ProvElementContentProvider());
		dialog.open();
		Object[] result = dialog.getResult();
		if (result != null && result.length > 0) {
			IProfile profile = ProvUI.getAdapter(result[0], IProfile.class);
			if (profile != null)
				return profile.getProfileId();
		}
		return null;
	}

	@Override
	protected void runCanceled() {
		super.runCanceled();
		userChosenProfileId = null;
	}
}
