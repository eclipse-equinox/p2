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

import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.AvailableUpdateElement;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;

public class UpdateWizardPage extends SizeComputingWizardPage {

	public static IInstallableUnit[] getIUsToReplace(Object[] replacementElements) {
		Set iusToReplace = new HashSet();
		for (int i = 0; i < replacementElements.length; i++) {
			if (replacementElements[i] instanceof AvailableUpdateElement) {
				iusToReplace.add(((AvailableUpdateElement) replacementElements[i]).getIUToBeUpdated());
			}
		}
		return (IInstallableUnit[]) iusToReplace.toArray(new IInstallableUnit[iusToReplace.size()]);
	}

	public static IInstallableUnit[] getReplacementIUs(Object[] replacementElements) {
		Set replacements = new HashSet();
		for (int i = 0; i < replacementElements.length; i++) {
			if (replacementElements[i] instanceof AvailableUpdateElement) {
				replacements.add(((AvailableUpdateElement) replacementElements[i]).getIU());
			}
		}
		return (IInstallableUnit[]) replacements.toArray(new IInstallableUnit[replacements.size()]);
	}

	public UpdateWizardPage(Policy policy, IUElementListRoot root, String profileId, ProvisioningPlan initialPlan) {
		super(policy, "UpdateWizardPage", root, profileId, initialPlan); //$NON-NLS-1$
		setTitle(ProvUIMessages.UpdateWizardPage_Title);
		setDescription(ProvUIMessages.UpdateWizardPage_Description);
	}

	protected ProfileChangeRequest computeProfileChangeRequest(Object[] selectedElements, MultiStatus additionalStatus, IProgressMonitor monitor) {
		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(getProfileId());
		request.removeInstallableUnits(getIUsToReplace(selectedElements));
		request.addInstallableUnits(getReplacementIUs(selectedElements));
		return request;
	}

	protected String getIUDescription(IInstallableUnit iu) {
		if (iu != null) {
			IUpdateDescriptor updateDescriptor = iu.getUpdateDescriptor();
			if (updateDescriptor != null && updateDescriptor.getDescription() != null && updateDescriptor.getDescription().length() > 0)
				return updateDescriptor.getDescription();
		}
		return super.getIUDescription(iu);
	}

	protected String getOperationLabel() {
		return ProvUIMessages.UpdateIUOperationLabel;
	}
}
