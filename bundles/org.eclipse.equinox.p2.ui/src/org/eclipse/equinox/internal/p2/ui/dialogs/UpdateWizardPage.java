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
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.AvailableUpdateElement;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.UpdateWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.viewers.CheckboxTableViewer;

public class UpdateWizardPage extends SizeComputingWizardPage {

	protected UpdateWizard wizard;
	AvailableUpdateElement[] updateElements;
	IInstallableUnit[] suggestedReplacements;
	Object[] initialSelections = new Object[0];

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

	public UpdateWizardPage(Policy policy, IInstallableUnit[] iusToReplace, AvailableUpdateElement[] elements, Object[] initialSelections, String profileId, ProvisioningPlan plan, UpdateWizard wizard) {
		super(policy, "UpdateWizardPage", iusToReplace, profileId, plan); //$NON-NLS-1$
		this.wizard = wizard;
		this.updateElements = elements;
		this.initialSelections = initialSelections;
		setTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
		setDescription(ProvUIMessages.UpdateAction_UpdatesAvailableMessage);
		computeSizing(plan, profileId);
	}

	// This method is removed to improve performance
	// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=221087
	/*
	protected IUColumnConfig[] getColumnConfig() {
		initializeDialogUnits(getShell());
		int pixels = convertHorizontalDLUsToPixels(DEFAULT_COLUMN_WIDTH);
		return new IUColumnConfig[] {new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, pixels), new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, pixels), new IUColumnConfig(ProvUIMessages.ProvUI_SizeColumnTitle, IUColumnConfig.COLUMN_SIZE, pixels / 2)};
	}
	*/

	protected void checkedIUsChanged() {
		// First ensure that a new plan is computed.
		super.checkedIUsChanged();
		// Now update the license page accordingly.  This requires the plan so
		// that licenses for required items can also be checked.
		wizard.planChanged(elementsToIUs(getCheckedElements()), currentPlan);
		// status of license page could change status of wizard next button
		// It no current page has been set yet (ie, we are still being created)
		// then the updateButtons() method will NPE.  This check is needed in
		// order to run the automated test cases.
		if (getContainer().getCurrentPage() != null)
			getContainer().updateButtons();
	}

	protected ProfileChangeRequest computeProfileChangeRequest(Object[] selectedElements, MultiStatus additionalStatus, IProgressMonitor monitor) {
		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(getProfileId());
		request.removeInstallableUnits(getIUsToReplace(selectedElements));
		request.addInstallableUnits(getReplacementIUs(selectedElements));
		return request;
	}

	protected void setInitialCheckState() {
		((CheckboxTableViewer) tableViewer).setCheckedElements(initialSelections);
	}

	protected String getIUDescription(IInstallableUnit iu) {
		if (iu != null) {
			IUpdateDescriptor updateDescriptor = iu.getUpdateDescriptor();
			if (updateDescriptor != null && updateDescriptor.getDescription() != null && updateDescriptor.getDescription().length() > 0)
				return updateDescriptor.getDescription();
		}
		return super.getIUDescription(iu);
	}

	protected void makeElements(IInstallableUnit[] iusToBeUpdated, List elements) {
		// ignore the originally selected ius, we want to use the
		// update elements computed by the creator of the wizard,
		// which contains both the elements to be replaced, and the
		// replacements
		for (int i = 0; i < updateElements.length; i++)
			elements.add(updateElements[i]);
	}

	protected String getOperationLabel() {
		return ProvUIMessages.UpdateIUOperationLabel;
	}
}
