/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.LicenseManager;
import org.eclipse.jface.wizard.Wizard;

/**
 * @since 3.4
 */
public abstract class UpdateOrInstallWizard extends Wizard {

	UpdateOrInstallWizardPage mainPage;
	AcceptLicensesWizardPage licensePage;
	Profile profile;
	IInstallableUnit[] ius;
	LicenseManager licenseManager;

	public UpdateOrInstallWizard(Profile profile, IInstallableUnit[] ius, LicenseManager licenseManager) {
		super();
		setForcePreviousAndNextButtons(true);
		setNeedsProgressMonitor(true);
		this.profile = profile;
		this.ius = ius;
		this.licenseManager = licenseManager;
	}

	public void addPages() {
		mainPage = createMainPage(profile, ius);
		addPage(mainPage);
		addPage(licensePage = new AcceptLicensesWizardPage(ius, licenseManager));
	}

	public boolean performFinish() {
		return mainPage.performFinish();
	}

	public void iusChanged(IInstallableUnit[] theIUs) {
		this.ius = theIUs;
		licensePage.update(ius);
	}

	protected abstract UpdateOrInstallWizardPage createMainPage(Profile theProfile, IInstallableUnit[] theIUs);

}
