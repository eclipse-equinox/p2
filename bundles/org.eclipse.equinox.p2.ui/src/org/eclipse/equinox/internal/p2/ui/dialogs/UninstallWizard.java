/*******************************************************************************
 *  Copyright (c) 2007, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *     Red Hat, Inc. - support for remediation page
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.equinox.internal.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.UninstallOperation;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.wizard.IWizardPage;

/**
 * @since 3.4
 */
public class UninstallWizard extends ProvisioningOperationWizard {

	public UninstallWizard(ProvisioningUI ui, UninstallOperation operation, Collection<IInstallableUnit> initialSelections, LoadMetadataRepositoryJob job) {
		super(ui, operation, initialSelections.toArray(), job);
		setWindowTitle(ProvUIMessages.UninstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_UNINSTALL));
	}

	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		mainPage = new SelectableIUsPage(ui, this, input, selections);
		mainPage.setTitle(ProvUIMessages.UninstallIUOperationLabel);
		mainPage.setDescription(ProvUIMessages.UninstallDialog_UninstallMessage);
		((SelectableIUsPage) mainPage).updateStatus(input, operation);
		return mainPage;
	}

	protected ResolutionResultsWizardPage createResolutionPage() {
		return new UninstallWizardPage(ui, this, root, (UninstallOperation) operation);
	}

	protected void initializeResolutionModelElements(Object[] selectedElements) {
		root = new IUElementListRoot(ui);
		ArrayList<InstalledIUElement> list = new ArrayList<InstalledIUElement>(selectedElements.length);
		ArrayList<InstalledIUElement> selections = new ArrayList<InstalledIUElement>(selectedElements.length);
		for (int i = 0; i < selectedElements.length; i++) {
			IInstallableUnit iu = ElementUtils.getIU(selectedElements[i]);
			if (iu != null) {
				InstalledIUElement element = new InstalledIUElement(root, getProfileId(), iu);
				list.add(element);
				selections.add(element);
			}
		}
		root.setChildren(list.toArray());
		planSelections = selections.toArray();
	}

	protected IResolutionErrorReportingPage createErrorReportingPage() {
		return (SelectableIUsPage) mainPage;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getStartingPage()
	 */
	public IWizardPage getStartingPage() {
		if (getCurrentStatus().isOK()) {
			((SelectableIUsPage) mainPage).setPageComplete(true);
			return resolutionPage;
		}
		return super.getStartingPage();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard#getProfileChangeOperation(java.lang.Object[])
	 */
	protected ProfileChangeOperation getProfileChangeOperation(Object[] elements) {
		UninstallOperation op = new UninstallOperation(ui.getSession(), ElementUtils.elementsToIUs(elements));
		op.setProfileId(getProfileId());
		//		op.setRootMarkerKey(getRootMarkerKey());
		op.setProvisioningContext(getProvisioningContext());
		return op;
	}

	@Override
	protected RemediationPage createRemediationPage() {
		// TODO Auto-generated method stub
		return null;
	}

}
