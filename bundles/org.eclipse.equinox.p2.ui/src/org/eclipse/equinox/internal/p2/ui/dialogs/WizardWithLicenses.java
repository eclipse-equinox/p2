/*******************************************************************************
 *  Copyright (c) 2008, 2013 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Dubrow <david.dubrow@nokia.com> - Bug 276356 [ui] check the wizard and page completion logic for AcceptLicensesWizardPage
 *     Sonatype, Inc. - ongoing development
 *     Ericsson AB (Hamdan Msheik) - Bypass install license wizard page via plugin_customization
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
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

	private static final String BYPASS_LICENSE_PAGE = "bypassLicensePage"; //$NON-NLS-1$

	AcceptLicensesWizardPage licensePage;
	boolean bypassLicensePage;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */

	public boolean isBypassLicensePage() {
		return bypassLicensePage;
	}

	public void setBypassLicensePage(boolean bypassLicensePage) {
		this.bypassLicensePage = bypassLicensePage;
	}

	@Override
	public void addPages() {
		super.addPages();

		if (!bypassLicensePage) {
			licensePage = createLicensesPage();
			addPage(licensePage);
		}
	}

	public WizardWithLicenses(ProvisioningUI ui, ProfileChangeOperation operation, Object[] initialSelections, LoadMetadataRepositoryJob job) {
		super(ui, operation, initialSelections, job);
		this.bypassLicensePage = canBypassLicensePage();
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
	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		// If the license page is supposed to be the next page,
		// ensure there are actually licenses that need acceptance.
		IWizardPage proposedPage = super.getNextPage(page);

		if (!bypassLicensePage) {
			if (proposedPage == licensePage && licensePage != null) {
				if (!licensePage.hasLicensesToAccept()) {
					proposedPage = null;
				} else {
					proposedPage = licensePage;
				}
			}
		}

		return proposedPage;
	}

	@Override
	protected void planChanged() {
		super.planChanged();
		if (!bypassLicensePage) {
			licensePage.update(ElementUtils.elementsToIUs(planSelections).toArray(new IInstallableUnit[0]), operation);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard#performFinish()
	 */
	@Override
	public boolean performFinish() {

		if (!bypassLicensePage) {
			licensePage.performFinish();
		}

		return super.performFinish();
	}

	public static boolean canBypassLicensePage() {
		IScopeContext[] contexts = new IScopeContext[] {InstanceScope.INSTANCE, DefaultScope.INSTANCE, BundleDefaultsScope.INSTANCE, ConfigurationScope.INSTANCE};
		boolean bypass = Platform.getPreferencesService().getBoolean(ProvUIActivator.PLUGIN_ID, BYPASS_LICENSE_PAGE, false, contexts);
		return bypass;
	}

}
