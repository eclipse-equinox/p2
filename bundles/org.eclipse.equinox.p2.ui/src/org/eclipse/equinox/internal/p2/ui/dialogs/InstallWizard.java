/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.equinox.internal.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.wizard.IWizardPage;

/**
 * An install wizard that allows the users to browse all of the repositories
 * and search/select for items to install.
 * 
 * @since 3.6
 */
public class InstallWizard extends WizardWithLicenses {

	SelectableIUsPage errorReportingPage;

	public InstallWizard(ProvisioningUI ui, InstallOperation operation, Collection<IInstallableUnit> initialSelections, LoadMetadataRepositoryJob preloadJob) {
		super(ui, operation, initialSelections == null ? null : initialSelections.toArray(), preloadJob);
		setWindowTitle(ProvUIMessages.InstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_INSTALL));
	}

	protected ResolutionResultsWizardPage createResolutionPage() {
		return new InstallWizardPage(ui, this, root, (InstallOperation) operation);
	}

	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		mainPage = new AvailableIUsPage(ui, this);
		if (selections != null && selections.length > 0)
			mainPage.setCheckedElements(selections);
		return mainPage;

	}

	protected void initializeResolutionModelElements(Object[] selectedElements) {
		if (selectedElements == null)
			return;
		root = new IUElementListRoot();
		ArrayList<AvailableIUElement> list = new ArrayList<AvailableIUElement>(selectedElements.length);
		ArrayList<AvailableIUElement> selections = new ArrayList<AvailableIUElement>(selectedElements.length);
		for (int i = 0; i < selectedElements.length; i++) {
			IInstallableUnit iu = ElementUtils.getIU(selectedElements[i]);
			if (iu != null) {
				AvailableIUElement element = new AvailableIUElement(root, iu, getProfileId(), shouldShowProvisioningPlanChildren());
				list.add(element);
				selections.add(element);
			}
		}
		root.setChildren(list.toArray());
		planSelections = selections.toArray();
	}

	protected ProvisioningContext getProvisioningContext() {
		return ((AvailableIUsPage) mainPage).getProvisioningContext();
	}

	protected IResolutionErrorReportingPage createErrorReportingPage() {
		if (root == null)
			errorReportingPage = new SelectableIUsPage(ui, this, null, null);
		else
			errorReportingPage = new SelectableIUsPage(ui, this, root, root.getChildren(root));
		errorReportingPage.setTitle(ProvUIMessages.InstallWizardPage_Title);
		errorReportingPage.setDescription(ProvUIMessages.PreselectedIUInstallWizard_Description);
		errorReportingPage.updateStatus(root, operation);
		return errorReportingPage;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getPreviousPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public IWizardPage getPreviousPage(IWizardPage page) {
		if (page == errorReportingPage) {
			mainPage.setCheckedElements(errorReportingPage.getCheckedIUElements());
			return mainPage;
		}
		return super.getPreviousPage(page);
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

	protected boolean shouldUpdateErrorPageModelOnPlanChange() {
		// We don't want the root of the error page to change unless we are on the
		// main page.  For example, if we are on the error page, change checkmarks, and
		// resolve again with an error, we wouldn't want the root items to change in the
		// error page.
		return getContainer().getCurrentPage() == mainPage && super.shouldUpdateErrorPageModelOnPlanChange();
	}
}
