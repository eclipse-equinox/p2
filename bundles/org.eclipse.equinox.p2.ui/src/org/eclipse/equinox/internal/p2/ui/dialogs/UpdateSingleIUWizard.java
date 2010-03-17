/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.ui.AcceptLicensesWizardPage;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * An update wizard that is invoked when there is only one thing to update, only
 * one update to choose, and the resolution is known to be successful.
 * 
 * @since 3.6
 */
public class UpdateSingleIUWizard extends WizardWithLicenses {

	public static boolean validFor(UpdateOperation operation) {
		return operation.hasResolved() && operation.getResolutionResult().isOK() && operation.getSelectedUpdates().length == 1;
	}

	public UpdateSingleIUWizard(ProvisioningUI ui, UpdateOperation operation) {
		super(ui, operation, null, null);
		setWindowTitle(ProvUIMessages.UpdateIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_UPDATE));
	}

	protected ISelectableIUsPage createMainPage() {
		mainPage = new UpdateSingleIUPage((UpdateOperation) operation, ui, this);
		mainPage.setTitle(ProvUIMessages.PreselectedIUInstallWizard_Title);
		mainPage.setDescription(ProvUIMessages.PreselectedIUInstallWizard_Description);
		return mainPage;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		mainPage = createMainPage();
		addPage(mainPage);
		AcceptLicensesWizardPage page = createLicensesPage();
		page.update(null, operation);
		if (page.hasLicensesToAccept())
			addPage(page);
	}

	protected void initializeResolutionModelElements(Object[] selectedElements) {
		root = new IUElementListRoot();
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
		op.setProvisioningContext(getProvisioningContext());
		return op;
	}

	@Override
	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		return createMainPage();
	}

	@Override
	protected ResolutionResultsWizardPage createResolutionPage() {
		return null;
	}
}
