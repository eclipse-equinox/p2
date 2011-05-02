/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Dubrow <david.dubrow@nokia.com> - Bug 276356 [ui] check the wizard and page completion logic for AcceptLicensesWizardPage
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.jface.wizard.IWizardPage;

/**
 * Common superclass for wizards that need to show licenses.
 * @since 3.5
 */
public abstract class WizardWithLicenses extends ProvisioningOperationWizard {

	AcceptLicensesWizardPage licensePage;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		super.addPages();
		licensePage = createLicensesPage();
		addPage(licensePage);
	}

	public WizardWithLicenses(ProvisioningUI ui, ProfileChangeOperation operation, Object[] initialSelections, LoadMetadataRepositoryJob job) {
		super(ui, operation, initialSelections, job);
	}

	protected AcceptLicensesWizardPage createLicensesPage() {
		IInstallableUnit[] ius = new IInstallableUnit[0];
		if (planSelections != null)
			ius = ElementUtils.elementsToIUs(planSelections).toArray(new IInstallableUnit[0]);
		return new AcceptLicensesWizardPage(ui.getLicenseManager(), ius, operation);
	}

	/*
	 * Overridden to determine whether the license page should be shown.
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public IWizardPage getNextPage(IWizardPage page) {
		// If the license page is supposed to be the next page,
		// ensure there are actually licenses that need acceptance.
		IWizardPage proposedPage = super.getNextPage(page);
		if (proposedPage != licensePage)
			return proposedPage;
		if (!licensePage.hasLicensesToAccept())
			return null;
		return licensePage;
	}

	protected void planChanged() {
		super.planChanged();
		licensePage.update(ElementUtils.elementsToIUs(planSelections).toArray(new IInstallableUnit[0]), operation);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard#performFinish()
	 */
	public boolean performFinish() {
		licensePage.performFinish();
		return super.performFinish();
	}
}
