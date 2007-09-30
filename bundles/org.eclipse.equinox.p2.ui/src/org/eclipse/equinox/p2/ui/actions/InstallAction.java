/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.ui.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.equinox.p2.ui.operations.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

public class InstallAction extends ProfileModificationAction {
	public static final int ENTRYPOINT_FORCE = 1;
	public static final int ENTRYPOINT_OPTIONAL = 2;
	public static final int ENTRYPOINT_NEVER = 3;
	int entryPointStrategy = ENTRYPOINT_FORCE;

	public InstallAction(String text, ISelectionProvider selectionProvider, IOperationConfirmer confirmer, Profile profile, IProfileChooser chooser, Shell shell) {
		super(text, selectionProvider, confirmer, profile, chooser, shell);
	}

	protected ProfileModificationOperation validateAndGetOperation(IInstallableUnit[] ius, Profile targetProfile, IProgressMonitor monitor) {
		// First validate whether the install can happen
		try {
			if (ProvisioningUtil.canInstall(ius, targetProfile, monitor, ProvUI.getUIInfoAdapter(getShell()))) {
				// Get a name for the entry point
				// TODO eventually this should be a specialized dialog for confirming an install, showing size, etc.
				String entryPointName = null;
				if (entryPointStrategy != ENTRYPOINT_NEVER) {
					entryPointName = getDefaultEntryPointName(ius);
					InputDialog dialog = new InputDialog(getShell(), ProvUIMessages.InstallAction_InstallConfirmTitle, ProvUIMessages.InstallAction_NameEntryPointMessage, entryPointName, new IInputValidator() {
						public String isValid(String string) {
							if (string.length() > 0 || entryPointStrategy == ENTRYPOINT_OPTIONAL)
								return null;
							return ProvUIMessages.InstallAction_EntryPointNameRequired;
						}
					});
					if (dialog.open() == Window.CANCEL)
						return null;
					entryPointName = dialog.getValue();
					if (entryPointName.length() == 0) {
						entryPointName = null;
					}
				}
				if (entryPointName == null) {
					if (entryPointStrategy == ENTRYPOINT_FORCE) {
						// shouldn't happen, but just in case
						return null;
					}
				}
				return new InstallOperation(ProvUIMessages.Ops_InstallIUOperationLabel, targetProfile.getProfileId(), ius, entryPointName);
			}
			MessageDialog.openInformation(getShell(), ProvUIMessages.InstallAction_InstallInfoTitle, ProvUIMessages.InstallAction_InstallNotPermitted);
		} catch (ProvisionException e) {
			// fall through and return null
		}
		return null;
	}

	private String getDefaultEntryPointName(IInstallableUnit[] ius) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < ius.length; i++) {
			result.append(ius[i].getId());
			if (i < ius.length - 1)
				result.append(", "); //$NON-NLS-1$
		}
		return result.toString();

	}

	public void setEntryPointStrategy(int strategy) {
		entryPointStrategy = strategy;
	}
}