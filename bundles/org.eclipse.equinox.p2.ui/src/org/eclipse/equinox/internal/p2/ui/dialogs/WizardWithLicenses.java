/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Dubrow <david.dubrow@nokia.com> - Bug 276356 [ui] check the wizard and page completion logic for AcceptLicensesWizardPage
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.AcceptLicensesWizardPage;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Display;

/**
 * Common superclass for wizards that need to show licenses.
 * @since 3.5
 */
public abstract class WizardWithLicenses extends ProvisioningOperationWizard {

	AcceptLicensesWizardPage licensePage;

	public WizardWithLicenses(Policy policy, String profileId, IUElementListRoot root, Object[] initialSelections, PlannerResolutionOperation initialResolution) {
		super(policy, profileId, root, initialSelections, initialResolution);
	}

	protected AcceptLicensesWizardPage createLicensesPage(IInstallableUnit[] ius, ProvisioningPlan plan) {
		return new AcceptLicensesWizardPage(policy, ius, plan);
	}

	public void addPages() {
		super.addPages();
	}

	public IWizardPage getNextPage(IWizardPage page) {
		if (page == resolutionPage) {
			if (licensePage == null) {
				licensePage = createLicensesPage(ElementUtils.elementsToIUs(mainPage.getCheckedIUElements()), resolutionPage.getCurrentPlan());
				addPage(licensePage);
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						IWizardContainer container = getContainer();
						if (container != null)
							container.updateButtons();
					}
				});

			}
			if (licensePage.hasLicensesToAccept()) {
				return licensePage;
			}
			return null;
		} else if (page == licensePage) {
			// we are done.  We explicitly code this because it's possible
			// that the license page is added to the wizard before a dynamic page that
			// gets added afterward, but should appear before.  
			return null;
		}
		return super.getNextPage(page);
	}

	protected void planChanged() {
		super.planChanged();
		if (resolutionOperation == null)
			return;
		if (licensePage == null) {
			licensePage = createLicensesPage(ElementUtils.elementsToIUs(mainPage.getCheckedIUElements()), resolutionOperation.getProvisioningPlan());
			addPage(licensePage);
		} else
			licensePage.update(ElementUtils.elementsToIUs(mainPage.getCheckedIUElements()), resolutionOperation.getProvisioningPlan());
		// Status of license page could change status of wizard next button
		// If no current page has been set yet (ie, we are still being created)
		// then the updateButtons() method will NPE.  This check is needed in
		// order to run the automated test cases.
		if (getContainer().getCurrentPage() != null)
			getContainer().updateButtons();
	}

	public boolean performFinish() {
		licensePage.performFinish();
		return super.performFinish();
	}
}
