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

import java.util.ArrayList;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.UpdateDialog;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.equinox.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

public class UpdateAction extends ProfileModificationAction {

	public UpdateAction(ISelectionProvider selectionProvider, Profile profile, IProfileChooser chooser, Shell shell) {
		super(ProvUI.UPDATE_COMMAND_LABEL, selectionProvider, profile, chooser, shell);
		setToolTipText(ProvUI.UPDATE_COMMAND_TOOLTIP);
	}

	protected ProfileModificationOperation validateAndGetOperation(IInstallableUnit[] ius, Profile targetProfile, IProgressMonitor monitor, IAdaptable uiInfo) {
		// Collect the replacements for each IU individually so that 
		// the user can decide what to update
		try {
			ArrayList iusWithUpdates = new ArrayList();
			for (int i = 0; i < ius.length; i++) {
				IInstallableUnit[] replacements = ProvisioningUtil.updatesFor(ius[i], monitor);
				if (replacements.length > 0)
					iusWithUpdates.add(ius[i]);
			}
			if (iusWithUpdates.size() > 0) {
				UpdateDialog dialog = new UpdateDialog(getShell(), (IInstallableUnit[]) iusWithUpdates.toArray(new IInstallableUnit[iusWithUpdates.size()]), targetProfile);
				dialog.open();
				return dialog.getOperation();
			}
			MessageDialog.openInformation(getShell(), ProvUIMessages.UpdateAction_UpdateInformationTitle, ProvUIMessages.UpdateOperation_NothingToUpdate);
		} catch (ProvisionException e) {
			// fall through and return null
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 *
	 *  Overridden to enable only on selections of installed IU's with the same parent
	 */
	public void selectionChanged(IStructuredSelection selection) {
		Object[] selectionArray = selection.toArray();
		Object parent = null;
		if (selectionArray.length > 0) {
			setEnabled(true);
			for (int i = 0; i < selectionArray.length; i++) {
				if (selectionArray[i] instanceof InstalledIUElement) {
					InstalledIUElement element = (InstalledIUElement) selectionArray[i];
					if (parent == null) {
						parent = element.getParent(null);
					} else if (parent != element.getParent(null)) {
						setEnabled(false);
						break;
					}
				} else {
					setEnabled(false);
					break;
				}
			}
		} else {
			setEnabled(false);
		}
	}

	protected String getTaskName() {
		return ProvUIMessages.UpdateIUProgress;
	}

}