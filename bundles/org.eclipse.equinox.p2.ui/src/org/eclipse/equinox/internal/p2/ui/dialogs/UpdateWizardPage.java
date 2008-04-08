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

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.AvailableUpdateElement;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;

public class UpdateWizardPage extends UpdateOrInstallWizardPage {
	AvailableUpdateElement[] updateElements;
	IInstallableUnit[] suggestedReplacements;
	Object[] initialSelections = new Object[0];

	public UpdateWizardPage(IInstallableUnit[] iusToReplace, AvailableUpdateElement[] elements, Object[] initialSelections, String profileId, ProvisioningPlan plan, UpdateOrInstallWizard wizard) {
		super("UpdateWizardPage", iusToReplace, profileId, plan, wizard); //$NON-NLS-1$
		this.updateElements = elements;
		this.initialSelections = initialSelections;
		setTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
		setDescription(ProvUIMessages.UpdateAction_UpdatesAvailableMessage);
	}

	protected void makeElements(IInstallableUnit[] ius, List elements) {
		for (int i = 0; i < updateElements.length; i++)
			elements.add(updateElements[i]);
	}

	private IInstallableUnit[] getIUsToReplace(Object[] replacementElements) {
		Set iusToReplace = new HashSet();
		for (int i = 0; i < replacementElements.length; i++) {
			if (replacementElements[i] instanceof AvailableUpdateElement) {
				iusToReplace.add(((AvailableUpdateElement) replacementElements[i]).getIUToBeUpdated());
			}
		}
		return (IInstallableUnit[]) iusToReplace.toArray(new IInstallableUnit[iusToReplace.size()]);
	}

	protected String getOperationLabel() {
		return ProvUIMessages.UpdateIUOperationLabel;
	}

	protected ProvisioningPlan computeProvisioningPlan(Object[] selectedElements, IProgressMonitor monitor) throws ProvisionException {
		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(getProfileId());
		request.removeInstallableUnits(getIUsToReplace(selectedElements));
		request.addInstallableUnits(elementsToIUs(selectedElements));
		ProvisioningPlan plan = ProvisioningUtil.getProvisioningPlan(request, getProvisioningContext(), monitor);
		computeSizing(plan, getProfileId());
		return plan;
	}

	protected void setInitialCheckState() {
		listViewer.setCheckedElements(initialSelections);
	}

	protected String getIUDescription(IInstallableUnit iu) {
		if (iu != null) {
			IUpdateDescriptor updateDescriptor = iu.getUpdateDescriptor();
			if (updateDescriptor != null && updateDescriptor.getDescription() != null && updateDescriptor.getDescription().length() > 0)
				return updateDescriptor.getDescription();
		}
		return super.getIUDescription(iu);
	}
}