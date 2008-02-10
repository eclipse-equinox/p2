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
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;

public class UninstallWizardPage extends ProfileModificationWizardPage {

	public UninstallWizardPage(IInstallableUnit[] ius, String profileId, ProvisioningPlan plan) {
		super("UninstallWizard", ius, profileId, plan); //$NON-NLS-1$
		setTitle(ProvUIMessages.UninstallIUOperationLabel);
		setDescription(ProvUIMessages.UninstallDialog_UninstallMessage);
	}

	protected ProvisioningPlan computeProvisioningPlan(Object[] selectedElements, IProgressMonitor monitor) {
		try {
			ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(getProfileId());
			request.removeInstallableUnits(elementsToIUs(selectedElements));
			return ProvisioningUtil.getProvisioningPlan(request, getProvisioningContext(), monitor);
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null);
		}
		return null;
	}

	protected String getOperationLabel() {
		return ProvUIMessages.UninstallIUOperationLabel;
	}
}