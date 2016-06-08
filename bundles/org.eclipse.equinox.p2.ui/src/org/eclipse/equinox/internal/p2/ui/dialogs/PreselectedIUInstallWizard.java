/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *     Red Hat, Inc. - support for remediation page
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.wizard.IWizardPage;

/**
 * An Install wizard that is invoked when the user has already selected which
 * IUs should be installed and does not need to browse the available software.
 * 
 * @since 3.5
 */
public class PreselectedIUInstallWizard extends WizardWithLicenses {

	QueryableMetadataRepositoryManager manager;

	public PreselectedIUInstallWizard(ProvisioningUI ui, InstallOperation operation, Collection<IInstallableUnit> initialSelections, LoadMetadataRepositoryJob job) {
		super(ui, operation, initialSelections.toArray(), job);
		setWindowTitle(ProvUIMessages.InstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_INSTALL));
	}

	@Override
	public IWizardPage getStartingPage() {
		if (remediationOperation != null && remediationOperation.getResolutionResult() == Status.OK_STATUS) {
			return getNextPage(mainPage);
		}
		return super.getStartingPage();
	}

	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		mainPage = new SelectableIUsPage(ui, this, input, selections);
		mainPage.setTitle(ProvUIMessages.PreselectedIUInstallWizard_Title);
		mainPage.setDescription(ProvUIMessages.PreselectedIUInstallWizard_Description);
		((SelectableIUsPage) mainPage).updateStatus(input, operation);
		return mainPage;
	}

	protected ResolutionResultsWizardPage createResolutionPage() {
		return new InstallWizardPage(ui, this, root, operation);
	}

	protected void initializeResolutionModelElements(Object[] selectedElements) {
		root = new IUElementListRoot(ui);
		ArrayList<AvailableIUElement> list = new ArrayList<AvailableIUElement>(selectedElements.length);
		ArrayList<AvailableIUElement> selected = new ArrayList<AvailableIUElement>(selectedElements.length);
		for (int i = 0; i < selectedElements.length; i++) {
			IInstallableUnit iu = ElementUtils.getIU(selectedElements[i]);
			if (iu != null) {
				AvailableIUElement element = new AvailableIUElement(root, iu, getProfileId(), shouldShowProvisioningPlanChildren());
				list.add(element);
				selected.add(element);
			}
		}
		root.setChildren(list.toArray());
		planSelections = selected.toArray();
		if (licensePage != null) {
			licensePage.update(ElementUtils.elementsToIUs(planSelections).toArray(new IInstallableUnit[0]), operation);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard#getErrorReportingPage()
	 */
	protected IResolutionErrorReportingPage createErrorReportingPage() {
		return (IResolutionErrorReportingPage) mainPage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard#getProfileChangeOperation(java.lang.Object[])
	 */
	protected ProfileChangeOperation getProfileChangeOperation(Object[] elements) {
		InstallOperation op = new InstallOperation(ui.getSession(), ElementUtils.elementsToIUs(elements));
		op.setProfileId(getProfileId());
		//		op.setRootMarkerKey(getRootMarkerKey());
		return op;
	}

	@Override
	protected RemediationPage createRemediationPage() {
		remediationPage = new RemediationPage(ui, this, root, operation);
		return remediationPage;
	}

}
