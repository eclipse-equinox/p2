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
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.InstallAction;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;

public class InstallWizardPage extends SizeComputingWizardPage {

	public InstallWizardPage(Policy policy, String profileId, IUElementListRoot root, ProvisioningPlan initialPlan) {
		super(policy, "InstallWizardPage", root, profileId, initialPlan); //$NON-NLS-1$
		setTitle(ProvUIMessages.InstallWizardPage_Title);
		setDescription(ProvUIMessages.InstallWizardPage_NoCheckboxDescription);
	}

	protected String getOperationLabel() {
		return ProvUIMessages.InstallIUOperationLabel;
	}

	protected ProfileChangeRequest computeProfileChangeRequest(Object[] selectedElements, MultiStatus additionalStatus, IProgressMonitor monitor) {
		IInstallableUnit[] selected = ElementUtils.elementsToIUs(selectedElements);
		return InstallAction.computeProfileChangeRequest(selected, getProfileId(), additionalStatus, monitor);
	}
}
