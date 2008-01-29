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
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.p2.ui.operations.ProvisioningUtil;

public class UninstallWizardPage extends ProfileModificationWizardPage {

	public UninstallWizardPage(IInstallableUnit[] ius, String profileId) {
		super("UninstallWizard", ius, profileId); //$NON-NLS-1$
		setTitle(ProvUIMessages.UninstallIUOperationLabel);
		setDescription(ProvUIMessages.UninstallDialog_UninstallMessage);
	}

	protected ProfileModificationOperation createProfileModificationOperation(Object[] selectedElements, IProgressMonitor monitor) {
		try {
			ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(getProfileId());
			request.removeInstallableUnits(elementsToIUs(selectedElements));
			ProvisioningPlan plan = ProvisioningUtil.getProvisioningPlan(request, monitor);
			IStatus status = plan.getStatus();
			if (status.isOK())
				return new ProfileModificationOperation(ProvUIMessages.UninstallIUOperationLabel, getProfileId(), plan);
			ProvUI.reportStatus(status);
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null);
		}
		return null;
	}

	protected String getOkButtonString() {
		return ProvUIMessages.UninstallIUOperationLabelWithMnemonic;
	}
}