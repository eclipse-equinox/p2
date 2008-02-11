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
import org.eclipse.equinox.internal.p2.ui.model.AvailableUpdateElement;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.LicenseManager;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;

/**
 * @since 3.4
 */
public class UpdateWizard extends UpdateOrInstallWizard {

	ProvisioningPlan plan;
	AvailableUpdateElement[] elements;
	Object[] initialSelections;

	public UpdateWizard(String profileId, IInstallableUnit[] iusToReplace, AvailableUpdateElement[] elements, Object[] initialSelections, ProvisioningPlan plan, LicenseManager licenseManager) {
		super(profileId, iusToReplace, licenseManager);
		setWindowTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_UPDATE));
		this.plan = plan;
		this.elements = elements;
		this.initialSelections = initialSelections;
	}

	protected UpdateOrInstallWizardPage createMainPage(String profileId, IInstallableUnit[] ius) {
		return new UpdateWizardPage(ius, elements, initialSelections, profileId, plan, this);
	}
}
