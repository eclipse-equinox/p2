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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.actions.ProfileModificationAction;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.IProfileChooser;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.dialogs.UninstallWizard;
import org.eclipse.equinox.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.p2.ui.operations.ProvisioningUtil;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

public class UninstallAction extends ProfileModificationAction {

	public UninstallAction(ISelectionProvider selectionProvider, String profileId, IProfileChooser chooser, Shell shell) {
		super(ProvUI.UNINSTALL_COMMAND_LABEL, selectionProvider, profileId, chooser, null, shell);
		setToolTipText(ProvUI.UNINSTALL_COMMAND_TOOLTIP);
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
		return ProvUIMessages.UninstallIUProgress;
	}

	protected void performOperation(IInstallableUnit[] ius, String targetProfileId) {
		UninstallWizard wizard = new UninstallWizard(targetProfileId, ius);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.open();
	}

	protected IStatus validateOperation(IInstallableUnit[] ius, String targetProfileId, IProgressMonitor monitor) {
		try {
			ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(targetProfileId);
			request.removeInstallableUnits(ius);
			ProvisioningPlan plan = ProvisioningUtil.getProvisioningPlan(request, monitor);
			return plan.getStatus();
		} catch (ProvisionException e) {
			return ProvUI.handleException(e, null);
		}

	}

}