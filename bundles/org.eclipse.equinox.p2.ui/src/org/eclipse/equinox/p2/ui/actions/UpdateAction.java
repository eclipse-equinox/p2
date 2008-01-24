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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.actions.ProfileModificationAction;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.equinox.p2.ui.dialogs.UpdateWizard;
import org.eclipse.equinox.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.p2.ui.query.IQueryProvider;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

public class UpdateAction extends ProfileModificationAction {

	IQueryProvider queryProvider;

	public UpdateAction(ISelectionProvider selectionProvider, String profileId, IProfileChooser chooser, LicenseManager licenseManager, IQueryProvider queryProvider, Shell shell) {
		super(ProvUI.UPDATE_COMMAND_LABEL, selectionProvider, profileId, chooser, licenseManager, shell);
		this.queryProvider = queryProvider;
		setToolTipText(ProvUI.UPDATE_COMMAND_TOOLTIP);
	}

	protected void performOperation(IInstallableUnit[] ius, String targetProfileId) {
		// Collect the replacements for each IU individually so that 
		// the user can decide what to update
		try {
			ArrayList iusWithUpdates = new ArrayList();
			for (int i = 0; i < ius.length; i++) {
				IInstallableUnit[] replacements = ProvisioningUtil.updatesFor(ius[i], null);
				if (replacements.length > 0)
					iusWithUpdates.add(ius[i]);
			}
			if (iusWithUpdates.size() > 0) {

				UpdateWizard wizard = new UpdateWizard(targetProfileId, (IInstallableUnit[]) iusWithUpdates.toArray(new IInstallableUnit[iusWithUpdates.size()]), getLicenseManager(), queryProvider);
				WizardDialog dialog = new WizardDialog(getShell(), wizard);
				dialog.open();
			}
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null);
		}
	}

	protected IStatus validateOperation(IInstallableUnit[] ius, String targetProfileId, IProgressMonitor monitor) {
		try {
			IInstallableUnit[] updates = ProvisioningUtil.updatesFor(ius, monitor);
			if (updates.length <= 0) {
				return new Status(IStatus.INFO, ProvUIActivator.PLUGIN_ID, ProvUIMessages.UpdateOperation_NothingToUpdate);
			}
			return Status.OK_STATUS;
		} catch (ProvisionException e) {
			return ProvUI.handleException(e, null);
		}

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