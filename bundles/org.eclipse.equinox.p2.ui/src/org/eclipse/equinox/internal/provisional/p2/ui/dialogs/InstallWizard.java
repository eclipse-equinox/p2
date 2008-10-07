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
package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.QueryableMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

/**
 * @since 3.4
 */
public class InstallWizard extends Wizard {

	Policy policy;
	AvailableIUsPage mainPage;
	InstallWizardPage resolutionPage;
	AcceptLicensesWizardPage licensePage;
	QueryableMetadataRepositoryManager manager;
	String profileId;
	IInstallableUnit[] ius;
	ProvisioningPlan plan;

	public InstallWizard(Policy policy, String profileId, IInstallableUnit[] ius, ProvisioningPlan plan, QueryableMetadataRepositoryManager manager) {
		this.policy = policy;
		this.profileId = profileId;
		this.plan = plan;
		this.ius = ius;
		this.manager = manager;
		setForcePreviousAndNextButtons(true);
		setNeedsProgressMonitor(true);
		setWindowTitle(ProvUIMessages.InstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_INSTALL));
	}

	public InstallWizard(Policy policy, String profileId) {
		this(policy, profileId, null, null, new QueryableMetadataRepositoryManager(policy, false));
	}

	protected InstallWizardPage createResolutionPage() {
		return new InstallWizardPage(policy, profileId, ius, plan, this);
	}

	protected AvailableIUsPage createMainPage() {
		return new AvailableIUsPage(policy, profileId, this, manager);
	}

	protected AcceptLicensesWizardPage createLicensesPage() {
		return new AcceptLicensesWizardPage(policy, ius, plan);
	}

	public void addPages() {
		// If the ius are already established, we don't need the first page
		if (ius == null)
			addPage(mainPage = createMainPage());
		addPage(resolutionPage = createResolutionPage());
		addPage(licensePage = createLicensesPage());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == mainPage) {
			resolutionPage.updateIUs();
			licensePage.update(resolutionPage.getSelectedIUs(), resolutionPage.getCurrentPlan());
			return resolutionPage;
		}
		if (page == resolutionPage && licensePage.hasLicensesToAccept())
			return licensePage;
		return null;
	}

	public boolean performFinish() {
		if (mainPage != null)
			mainPage.performFinish();
		licensePage.performFinish();
		return resolutionPage.performFinish();
	}

	public IInstallableUnit[] getCheckedIUs() {
		if (mainPage == null)
			return resolutionPage.getSelectedIUs();
		return mainPage.getCheckedIUs();
	}
}
