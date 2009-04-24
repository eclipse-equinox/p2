/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import java.util.ArrayList;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.QueryableMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.InstallAction;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;

/**
 * An Install wizard that is invoked when the user has already selected which
 * IUs should be installed and does not need to browse the available software.
 * 
 * @since 3.5
 */
public class PreselectedIUInstallWizard extends WizardWithLicenses {

	SelectableIUsPage mainPage;
	QueryableMetadataRepositoryManager manager;

	static IUElementListRoot makeElementRoot(IInstallableUnit[] ius, String profileId) {
		IUElementListRoot elementRoot = new IUElementListRoot();
		Object[] elements = new Object[ius.length];
		for (int i = 0; i < ius.length; i++) {
			if (ius[i] != null)
				elements[i] = new AvailableIUElement(elementRoot, ius[i], profileId, false);
		}
		elementRoot.setChildren(elements);
		return elementRoot;
	}

	public PreselectedIUInstallWizard(Policy policy, String profileId, IInstallableUnit[] initialSelections, PlannerResolutionOperation initialResolution, QueryableMetadataRepositoryManager manager) {
		super(policy, profileId, makeElementRoot(initialSelections, profileId), initialSelections, initialResolution);
		this.manager = manager;
		setWindowTitle(ProvUIMessages.InstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_INSTALL));
	}

	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		mainPage = new SelectableIUsPage(policy, input, selections, profileId);
		mainPage.setTitle(ProvUIMessages.PreselectedIUInstallWizard_Title);
		mainPage.setDescription(ProvUIMessages.PreselectedIUInstallWizard_Description);
		mainPage.updateStatus(input, resolutionOperation);
		return mainPage;
	}

	protected ResolutionResultsWizardPage createResolutionPage() {
		return new InstallWizardPage(policy, profileId, root, resolutionOperation);
	}

	protected IUElementListRoot makeResolutionElementRoot(Object[] selectedElements) {
		IUElementListRoot elementRoot = new IUElementListRoot();
		ArrayList list = new ArrayList(selectedElements.length);
		for (int i = 0; i < selectedElements.length; i++) {
			IInstallableUnit iu = ElementUtils.getIU(selectedElements[i]);
			if (iu != null)
				list.add(new AvailableIUElement(elementRoot, iu, profileId, policy.getQueryContext().getShowProvisioningPlanChildren()));
		}
		elementRoot.setChildren(list.toArray());
		return elementRoot;
	}

	protected ProfileChangeRequest computeProfileChangeRequest(Object[] selectedElements, MultiStatus additionalStatus, IProgressMonitor monitor) {
		IInstallableUnit[] selected = ElementUtils.elementsToIUs(selectedElements);
		return InstallAction.computeProfileChangeRequest(selected, profileId, additionalStatus, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard#getErrorReportingPage()
	 */
	protected IResolutionErrorReportingPage getErrorReportingPage() {
		return mainPage;
	}
}
