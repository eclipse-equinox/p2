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
import org.eclipse.equinox.internal.p2.ui.InstallDialog;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.equinox.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.p2.ui.operations.ProvisioningUtil;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

public class InstallAction extends ProfileModificationAction {

	public InstallAction(ISelectionProvider selectionProvider, Profile profile, IProfileChooser chooser, Shell shell) {
		super(ProvUI.INSTALL_COMMAND_LABEL, selectionProvider, profile, chooser, shell);
		setToolTipText(ProvUI.INSTALL_COMMAND_TOOLTIP);
	}

	protected ProfileModificationOperation validateAndGetOperation(IInstallableUnit[] ius, Profile targetProfile, IProgressMonitor monitor) {
		// First validate whether the install can happen
		try {
			ProvisioningPlan plan = ProvisioningUtil.getInstallPlan(ius, targetProfile, monitor);
			IStatus status = plan.getStatus();
			if (status.isOK()) {
				InstallDialog dialog = new InstallDialog(getShell(), ius, targetProfile);
				dialog.open();
				return dialog.getOperation();
			}
			ProvUI.reportStatus(status);
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null);
			// fall through and return null
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 *
	 *  Overridden to enable only on selections with IU's.  Does not validate
	 *  whether the IU is already installed in a particular profile.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		Object[] selectionArray = selection.toArray();
		if (selectionArray.length < 1) {
			setEnabled(false);
		} else {
			for (int i = 0; i < selectionArray.length; i++) {
				if (!(selectionArray[i] instanceof IInstallableUnit)) {
					setEnabled(false);
					return;
				}
			}
			setEnabled(true);
		}
	}

	protected String getTaskName() {
		return ProvUIMessages.InstallIUProgress;
	}
}