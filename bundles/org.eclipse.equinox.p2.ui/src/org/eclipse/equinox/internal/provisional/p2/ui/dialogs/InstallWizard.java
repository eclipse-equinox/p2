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
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.QueryableMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.InstallAction;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Composite;

/**
 * An install wizard that allows the users to browse all of the repositories
 * and search/select for items to install.
 * 
 * @since 3.4
 */
public class InstallWizard extends WizardWithLicenses {

	QueryableMetadataRepositoryManager manager;
	AvailableIUsPage mainPage;
	SelectableIUsPage errorReportingPage;

	public InstallWizard(Policy policy, String profileId, IInstallableUnit[] initialSelections, PlannerResolutionOperation initialResolution, QueryableMetadataRepositoryManager manager) {
		super(policy, profileId, null, initialSelections, initialResolution);
		this.manager = manager;
		setWindowTitle(ProvUIMessages.InstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_INSTALL));
	}

	public InstallWizard(Policy policy, String profileId) {
		this(policy, profileId, null, null, new QueryableMetadataRepositoryManager(policy.getQueryContext(), false));
	}

	protected ResolutionResultsWizardPage createResolutionPage() {
		return new InstallWizardPage(policy, profileId, root, resolutionOperation);
	}

	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		mainPage = new AvailableIUsPage(policy, profileId, manager);
		if (selections != null && selections.length > 0)
			mainPage.setCheckedElements(selections);
		return mainPage;

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

	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		if (manager != null)
			// async exec since we are in the middle of opening
			pageContainer.getDisplay().asyncExec(new Runnable() {
				public void run() {
					manager.reportAccumulatedStatus();
				}
			});
	}

	protected ProvisioningContext getProvisioningContext() {
		return mainPage.getProvisioningContext();
	}

	protected ProfileChangeRequest computeProfileChangeRequest(Object[] selectedElements, MultiStatus additionalStatus, IProgressMonitor monitor) {
		IInstallableUnit[] selected = ElementUtils.elementsToIUs(selectedElements);
		return InstallAction.computeProfileChangeRequest(selected, profileId, additionalStatus, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard#getErrorReportingPage()
	 */
	protected IResolutionErrorReportingPage getErrorReportingPage() {
		if (errorReportingPage == null) {
			originalRoot = root;
			errorReportingPage = new SelectableIUsPage(policy, root, root.getChildren(root), profileId);
			errorReportingPage.setTitle(ProvUIMessages.InstallWizardPage_Title);
			errorReportingPage.setDescription(ProvUIMessages.PreselectedIUInstallWizard_Description);
			errorReportingPage.updateStatus(root, resolutionOperation);
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
			errorReportingPage.updateStatus(originalRoot, resolutionOperation);
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
}
