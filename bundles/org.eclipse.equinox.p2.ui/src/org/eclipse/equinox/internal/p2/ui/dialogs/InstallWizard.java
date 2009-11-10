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
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Composite;

/**
 * An install wizard that allows the users to browse all of the repositories
 * and search/select for items to install.
 * 
 * @since 3.6
 */
public class InstallWizard extends WizardWithLicenses {

	AvailableIUsPage mainPage;
	SelectableIUsPage errorReportingPage;

	public InstallWizard(ProvisioningUI ui, InstallOperation operation, IInstallableUnit[] initialSelections, PreloadMetadataRepositoryJob preloadJob) {
		super(ui, operation, initialSelections, preloadJob);
		setWindowTitle(ProvUIMessages.InstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_INSTALL));
	}

	protected ResolutionResultsWizardPage createResolutionPage() {
		return new InstallWizardPage(ui, root, (InstallOperation) operation);
	}

	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		mainPage = new AvailableIUsPage(ui);
		if (selections != null && selections.length > 0)
			mainPage.setCheckedElements(selections);
		return mainPage;

	}

	protected IUElementListRoot makeResolutionElementRoot(Object[] selectedElements) {
		if (selectedElements == null)
			return null;
		IUElementListRoot elementRoot = new IUElementListRoot();
		ArrayList list = new ArrayList(selectedElements.length);
		for (int i = 0; i < selectedElements.length; i++) {
			IInstallableUnit iu = ElementUtils.getIU(selectedElements[i]);
			if (iu != null)
				list.add(new AvailableIUElement(elementRoot, iu, getProfileId(), getPolicy().getQueryContext().getShowProvisioningPlanChildren()));
		}
		elementRoot.setChildren(list.toArray());
		return elementRoot;
	}

	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		if (repoPreloadJob != null)
			// async exec since we are in the middle of opening
			pageContainer.getDisplay().asyncExec(new Runnable() {
				public void run() {
					repoPreloadJob.reportAccumulatedStatus();
				}
			});
	}

	protected ProvisioningContext getProvisioningContext() {
		return mainPage.getProvisioningContext();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard#getErrorReportingPage()
	 */
	protected IResolutionErrorReportingPage getErrorReportingPage() {
		if (errorReportingPage == null) {
			originalRoot = root;
			errorReportingPage = new SelectableIUsPage(ui, root, root.getChildren(root));
			errorReportingPage.setTitle(ProvUIMessages.InstallWizardPage_Title);
			errorReportingPage.setDescription(ProvUIMessages.PreselectedIUInstallWizard_Description);
			errorReportingPage.updateStatus(root, operation);
			errorReportingPage.setCheckedElements(root.getChildren(root));
			addPage(errorReportingPage);
		}
		return errorReportingPage;
	}

	protected void showingErrorPage() {
		// If we did a new resolution and are showing the error page,
		// update the root.  We don't do this when the page is not the main
		// page, or we might be updating the root of the showing page.
		if (getContainer().getCurrentPage() == mainPage) {
			originalRoot = root;
			errorReportingPage.updateStatus(originalRoot, operation);
			errorReportingPage.setCheckedElements(root.getChildren(root));
		}
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
		op.setRootMarkerKey(getRootMarkerKey());
		return op;
	}
}
