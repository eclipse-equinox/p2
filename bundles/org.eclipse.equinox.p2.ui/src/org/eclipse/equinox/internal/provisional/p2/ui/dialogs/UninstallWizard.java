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
import org.eclipse.equinox.internal.p2.ui.dialogs.UninstallWizardPage;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.wizard.Wizard;

/**
 * @since 3.4
 */
public class UninstallWizard extends Wizard {

	UninstallWizardPage page;
	Policy policy;
	String profileId;
	IInstallableUnit[] ius;
	ProvisioningPlan plan;

	public UninstallWizard(Policy policy, String profileId, IInstallableUnit[] ius, ProvisioningPlan initialProvisioningPlan) {
		super();
		this.policy = policy;
		setWindowTitle(ProvUIMessages.UninstallIUOperationLabel);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_UNINSTALL));
		this.profileId = profileId;
		this.ius = ius;
		this.plan = initialProvisioningPlan;
	}

	public void addPages() {
		page = new UninstallWizardPage(policy, ius, profileId, plan);
		addPage(page);
	}

	public boolean performFinish() {
		return page.performFinish();
	}

}
