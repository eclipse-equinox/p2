/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB (Hamdan Msheik) - Bypass install license wizard page via plugin_customization
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.ui.AcceptLicensesWizardPage;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.wizard.Wizard;

/**
 * An update wizard that is invoked when there is only one thing to update, only
 * one update to choose, and the resolution is known to be successful.
 *
 * @since 3.6
 */
public class UpdateSingleIUWizard extends Wizard {

	UpdateSingleIUPage mainPage;
	ProvisioningUI ui;
	UpdateOperation operation;

	public static boolean validFor(UpdateOperation operation) {
		return operation.hasResolved() && operation.getResolutionResult().isOK() && operation.getSelectedUpdates().length == 1;
	}

	public UpdateSingleIUWizard(ProvisioningUI ui, UpdateOperation operation) {
		super();
		this.ui = ui;
		this.operation = operation;
		setWindowTitle(ProvUIMessages.UpdateIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_UPDATE));
	}

	protected UpdateSingleIUPage createMainPage() {
		mainPage = new UpdateSingleIUPage(operation, ui);
		return mainPage;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		mainPage = createMainPage();
		addPage(mainPage);

		if (!WizardWithLicenses.canBypassLicencePage()) {
			AcceptLicensesWizardPage page = createLicensesPage();
			page.update(null, operation);
			if (page.hasLicensesToAccept())
				addPage(page);
		}
	}

	protected AcceptLicensesWizardPage createLicensesPage() {
		return new AcceptLicensesWizardPage(ui.getLicenseManager(), null, operation);
	}

	@Override
	public boolean performFinish() {
		return mainPage.performFinish();
	}

}
