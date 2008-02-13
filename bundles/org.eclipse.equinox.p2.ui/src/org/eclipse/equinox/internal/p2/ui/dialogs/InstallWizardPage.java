/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;

public class InstallWizardPage extends UpdateOrInstallWizardPage {

	public InstallWizardPage(IInstallableUnit[] ius, String profileId, ProvisioningPlan plan, UpdateOrInstallWizard wizard) {
		super("InstallWizardPage", ius, profileId, plan, wizard); //$NON-NLS-1$
		setTitle(ProvUIMessages.InstallIUOperationLabel);
		setDescription(ProvUIMessages.InstallDialog_InstallSelectionMessage);
	}

	protected String getOperationLabel() {
		return ProvUIMessages.InstallIUOperationLabel;
	}

	protected ProvisioningPlan computeProvisioningPlan(Object[] selectedElements, IProgressMonitor monitor) throws ProvisionException {
		IInstallableUnit[] selected = elementsToIUs(selectedElements);
		ProfileChangeRequest changeRequest = ProfileChangeRequest.createByProfileId(getProfileId());
		changeRequest.addInstallableUnits(selected);
		for (int i = 0; i < selected.length; i++) {
			changeRequest.setInstallableUnitProfileProperty(selected[i], IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
		}
		return ProvisioningUtil.getProvisioningPlan(changeRequest, getProvisioningContext(), monitor);
	}
}
