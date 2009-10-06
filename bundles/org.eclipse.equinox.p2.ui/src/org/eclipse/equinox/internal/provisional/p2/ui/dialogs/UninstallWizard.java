/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import java.util.ArrayList;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.wizard.IWizardPage;

/**
 * @since 3.4
 */
public class UninstallWizard extends ProvisioningOperationWizard {

	SelectableIUsPage mainPage;

	static IUElementListRoot makeElementRoot(Object[] selectedElements, String profileId) {
		IUElementListRoot elementRoot = new IUElementListRoot();
		ArrayList list = new ArrayList(selectedElements.length);
		for (int i = 0; i < selectedElements.length; i++) {
			IInstallableUnit iu = ElementUtils.getIU(selectedElements[i]);
			if (iu != null)
				list.add(new InstalledIUElement(elementRoot, profileId, iu));
		}
		elementRoot.setChildren(list.toArray());
		return elementRoot;
	}

	public UninstallWizard(Policy policy, String profileId, IInstallableUnit[] ius, PlannerResolutionOperation initialResolution) {
		super(policy, profileId, makeElementRoot(ius, profileId), ius, initialResolution);
		setWindowTitle(ProvUIMessages.UninstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_UNINSTALL));
	}

	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		mainPage = new SelectableIUsPage(policy, input, selections, profileId);
		mainPage.setTitle(ProvUIMessages.UninstallIUOperationLabel);
		mainPage.setDescription(ProvUIMessages.UninstallDialog_UninstallMessage);
		mainPage.updateStatus(input, resolutionOperation);
		return mainPage;
	}

	protected ResolutionResultsWizardPage createResolutionPage() {
		return new UninstallWizardPage(policy, root, profileId, resolutionOperation);
	}

	protected IUElementListRoot makeResolutionElementRoot(Object[] selectedElements) {
		return makeElementRoot(selectedElements, profileId);
	}

	protected ProfileChangeRequest computeProfileChangeRequest(Object[] selectedElements, MultiStatus additionalStatus, IProgressMonitor monitor) {
		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(profileId);
		request.removeInstallableUnits(ElementUtils.elementsToIUs(selectedElements));
		return request;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard#getErrorReportingPage()
	 */
	protected IResolutionErrorReportingPage getErrorReportingPage() {
		return mainPage;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getStartingPage()
	 */
	public IWizardPage getStartingPage() {
		if (getCurrentStatus().isOK()) {
			if (resolutionPage == null) {
				resolutionPage = createResolutionPage();
				addPage(resolutionPage);
			}
			mainPage.setPageComplete(true);
			return resolutionPage;
		}
		return super.getStartingPage();
	}
}
