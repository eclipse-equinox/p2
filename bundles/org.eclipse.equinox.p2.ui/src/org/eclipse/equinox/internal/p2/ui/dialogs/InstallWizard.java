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
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
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

	SelectableIUsPage errorReportingPage;
	IUElementListRoot originalRoot;

	public InstallWizard(ProvisioningUI ui, InstallOperation operation, IInstallableUnit[] initialSelections, LoadMetadataRepositoryJob preloadJob) {
		super(ui, operation, initialSelections, preloadJob);
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
		ArrayList list = new ArrayList(selectedElements.length);
		ArrayList selections = new ArrayList(selectedElements.length);
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

	protected void planChanged() {
		// the superclass may change the page root when we don't wish this to happen.
		// The code below will correct that case.  We set redraw to avoid a big flash.
		errorReportingPage.getControl().setRedraw(false);
		try {
			super.planChanged();
			// We don't want the root of the error page to change unless we are on the
			// main page.  For example, if we are on the error page, change checkmarks, and
			// resolve again with an error, we wouldn't want the root items to change in the
			// error page.
			if (getContainer().getCurrentPage() == mainPage) {
				originalRoot = root;
			} else {
				errorReportingPage.updateStatus(originalRoot, operation);
			}
			// we always update the checkmarks to the current root
			errorReportingPage.setCheckedElements(root.getChildren(root));
		} finally {
			errorReportingPage.getControl().setRedraw(true);
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
		//		op.setRootMarkerKey(getRootMarkerKey());
		return op;
	}
}
