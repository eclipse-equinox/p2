/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.AcceptLicensesWizardPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.UpdateWizardPage;
import org.eclipse.equinox.internal.p2.ui.model.AvailableUpdateElement;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

/**
 * @since 3.4
 */
public class UpdateWizard extends Wizard {
	UpdateWizardPage mainPage;
	Policy policy;
	AcceptLicensesWizardPage licensePage;
	protected String profileId;
	protected IInstallableUnit[] ius;
	ProvisioningPlan plan;
	AvailableUpdateElement[] elements;
	Object[] initialSelections;

	public UpdateWizard(Policy policy, String profileId, IInstallableUnit[] iusToReplace, AvailableUpdateElement[] elements, Object[] initialSelections, ProvisioningPlan plan) {
		super();
		this.policy = policy;
		setForcePreviousAndNextButtons(true);
		setNeedsProgressMonitor(true);
		this.profileId = profileId;
		this.ius = iusToReplace;
		setWindowTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_UPDATE));
		this.plan = plan;
		this.elements = elements;
		this.initialSelections = initialSelections;
	}

	public void addPages() {
		addPage(mainPage = createMainPage());
		addPage(licensePage = createLicensesPage());
	}

	protected UpdateWizardPage createMainPage() {
		return new UpdateWizardPage(policy, ius, elements, initialSelections, profileId, plan, this);
	}

	protected AcceptLicensesWizardPage createLicensesPage() {
		return new AcceptLicensesWizardPage(policy, UpdateWizardPage.getReplacementIUs(initialSelections), plan);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == mainPage && licensePage.hasLicensesToAccept())
			return licensePage;
		return null;
	}

	public boolean performFinish() {
		licensePage.performFinish();
		return mainPage.performFinish();
	}

	public void planChanged(IInstallableUnit[] selectedIUs, ProvisioningPlan newPlan) {
		this.ius = selectedIUs;
		licensePage.update(selectedIUs, newPlan);
	}
}
